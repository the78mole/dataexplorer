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
    
    Copyright (c) 2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class GPSLoggerDialog extends DeviceDialog {
	final static Logger						log									= Logger.getLogger(GPSLoggerDialog.class.getName());

	CTabFolder										tabFolder, subTabFolder1, subTabFolder2;
	CTabItem											visualizationTabItem, configurationTabItem, uniLogTabItem, mLinkTabItem;
	Composite											visualizationMainComposite, uniLogVisualization, mLinkVisualization;
	Composite											configurationMainComposite;
	GPSLoggerSetupConfiguration1	configuration1Composite;
	GPSLoggerSetupConfiguration2	configuration2Composite;

	Button												saveVisualizationButton, inputFileButton, helpButton, saveSetupButton, closeButton;

	CTabItem											gpsLoggerTabItem, telemetryTabItem;

	final GPSLogger								device;																																						// get device specific things, get serial port, ...
	final Settings								settings;																																					// application configuration settings
	SetupReaderWriter							loggerSetup;

	RecordSet											lastActiveRecordSet		= null;
	boolean												isVisibilityChanged	= false;
	int														measurementsCount		= 0;
	final List<CTabItem>					configurations			= new ArrayList<CTabItem>();

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public GPSLoggerDialog(Shell parent, GPSLogger useDevice) {
		super(parent);
		this.device = useDevice;
		this.settings = Settings.getInstance();
		for (int i = 1; i <= this.device.getChannelCount(); i++) {
			this.measurementsCount = 15; //15 measurements are displayed as maximum per visualization tab
		}
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
				this.loggerSetup = new SetupReaderWriter(this.dialogShell, this.device);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(650, 30 + 25 + 25 + this.measurementsCount * 29 + 50 + 42); //header + tab + label + this.measurementsCount * 23 + buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (GPSLoggerDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { GPSLoggerDialog.this.device.getPropertiesFileName() });
							if (GPSLoggerDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								GPSLoggerDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						GPSLoggerDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						GPSLoggerDialog.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html");  //$NON-NLS-1$
					}
				});
				this.dialogShell.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent paintevent) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "dialogShell.paintControl, event=" + paintevent); //$NON-NLS-1$
						RecordSet activeRecordSet = GPSLoggerDialog.this.application.getActiveRecordSet();
						if (GPSLoggerDialog.this.lastActiveRecordSet == null && activeRecordSet != null 
								|| ( activeRecordSet != null && !GPSLoggerDialog.this.lastActiveRecordSet.getName().equals(activeRecordSet.getName()))) {
							GPSLoggerDialog.this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
						}
						GPSLoggerDialog.this.lastActiveRecordSet = GPSLoggerDialog.this.application.getActiveRecordSet();
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					this.tabFolder.setSimple(false);
					{
						//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
						//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
						//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
						//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
						createVisualizationTabItem(1, 15, 9, 15);
						//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
						//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
						//Unilog2 15=Voltage, 16=Current, 17=Capacity, 18=Power, 19=Energy, 20=CellBalance, 21=CellVoltage1, 21=CellVoltage2, 23=CellVoltage3, 
						//Unilog2 24=CellVoltage4, 25=CellVoltage5, 26=CellVoltage6, 27=Revolution, 28=ValueA1, 29=ValueA2, 30=ValueA3, 31=InternTemperature
						//M-LINK  32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
						createVisualizationTabItem(2, 15, 17, 15);
					}
					{
						this.configurationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
						this.configurationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.configurationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2015));

						this.configurationMainComposite = new Composite(this.tabFolder, SWT.NONE);
						FormLayout configurationMainCompositeLayout = new FormLayout();
						this.configurationMainComposite.setLayout(configurationMainCompositeLayout);
						this.configurationTabItem.setControl(this.configurationMainComposite);
						{
							FormData layoutConfig1Data = new FormData();
							layoutConfig1Data.top = new FormAttachment(0, 1000, 0);
							layoutConfig1Data.left = new FormAttachment(0, 1000, 0);
							layoutConfig1Data.right = new FormAttachment(493, 1000, 0);
							layoutConfig1Data.bottom = new FormAttachment(1000, 1000, 0);
							this.configuration1Composite = new GPSLoggerSetupConfiguration1(this.configurationMainComposite, SWT.NONE, this, this.loggerSetup);
							this.configuration1Composite.setLayoutData(layoutConfig1Data);
						}
						{
							FormData layoutConfig2Data = new FormData();
							layoutConfig2Data.top = new FormAttachment(0, 1000, 0);
							layoutConfig2Data.left = new FormAttachment(493, 1000, 0);
							layoutConfig2Data.right = new FormAttachment(1000, 1000, 0);
							layoutConfig2Data.bottom = new FormAttachment(1000, 1000, 0);
							this.configuration2Composite = new GPSLoggerSetupConfiguration2(this.configurationMainComposite, SWT.NONE, this, this.loggerSetup);
							this.configuration2Composite.setLayoutData(layoutConfig2Data);
						}
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.setSelection(0);
					this.tabFolder.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							if (GPSLoggerDialog.this.tabFolder.getSelectionIndex() == GPSLoggerDialog.this.tabFolder.getItemCount()-1) 
								GPSLoggerDialog.this.loggerSetup.loadSetup();
							GPSLoggerDialog.this.configuration1Composite.updateValues();
							GPSLoggerDialog.this.configuration2Composite.updateValues();
						}
					});

				}
				{
					this.saveVisualizationButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 130;
					saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveButtonLData.left = new FormAttachment(0, 1000, 15);
					saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveVisualizationButton.setLayoutData(saveButtonLData);
					this.saveVisualizationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveVisualizationButton.setText(Messages.getString(MessageIds.GDE_MSGT2016));
					this.saveVisualizationButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2023));
					this.saveVisualizationButton.setEnabled(false);
					this.saveVisualizationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerDialog.this.device.storeDeviceProperties();
							GPSLoggerDialog.this.saveVisualizationButton.setEnabled(false);
						}
					});
				}
				{
					this.inputFileButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData inputFileButtonLData = new FormData();
					inputFileButtonLData.width = 130;
					inputFileButtonLData.height = GDE.IS_MAC ? 33 : 30;
					inputFileButtonLData.left = new FormAttachment(0, 1000, 155);
					inputFileButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.inputFileButton.setLayoutData(inputFileButtonLData);
					this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT2017));
					this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2025));
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerDialog.this.isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { GPSLoggerDialog.this.device.getPropertiesFileName() });
								if (GPSLoggerDialog.this.application.openYesNoMessageDialog(GPSLoggerDialog.this.dialogShell, msg) == SWT.YES) {
									log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
									GPSLoggerDialog.this.device.storeDeviceProperties();
								}
							}
							GPSLoggerDialog.this.device.open_closeCommPort();
						}
					});
				}
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData helpButtonLData = new FormData();
					helpButtonLData.width = GDE.IS_MAC ? 50 : 40;
					helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
					helpButtonLData.left = new FormAttachment(0, 1000, 302);
					helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.helpButton.setLayoutData(helpButtonLData);
					this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerDialog.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html");  //$NON-NLS-1$
						}
					});
				}
				{
					this.saveSetupButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveSetupButtonLData = new FormData();
					saveSetupButtonLData.width = 130;
					saveSetupButtonLData.height = GDE.IS_MAC ? 33 : 30;
					saveSetupButtonLData.right = new FormAttachment(1000, 1000, -155);
					saveSetupButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
					this.saveSetupButton.setLayoutData(saveSetupButtonLData);
					this.saveSetupButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveSetupButton.setText(Messages.getString(MessageIds.GDE_MSGT2018));
					this.saveSetupButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2024));
					this.saveSetupButton.setEnabled(false);
					this.saveSetupButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "saveSetupButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerDialog.this.loggerSetup.saveSetup();
							GPSLoggerDialog.this.saveSetupButton.setEnabled(false);
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData closeButtonLData = new FormData();
					closeButtonLData.width = 130;
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
							GPSLoggerDialog.this.dispose();
						}
					});
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 375, 10));
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
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * create a visualization control tab item
	 * @param channelNumber
	 */
	private void createVisualizationTabItem(int channelNumber, int numMeasurements, int numMeasurements_UL, int numMeasurements_ML) {
		this.visualizationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		this.visualizationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.visualizationTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2009) + GDE.STRING_MESSAGE_CONCAT + this.device.getChannelName(channelNumber));

		this.visualizationMainComposite = new Composite(this.tabFolder, SWT.NONE);
		FormLayout visualizationMainCompositeLayout = new FormLayout();
		this.visualizationMainComposite.setLayout(visualizationMainCompositeLayout);
		this.visualizationTabItem.setControl(this.visualizationMainComposite);
		{
			FormData layoutData = new FormData();
			layoutData.top = new FormAttachment(0, 1000, 0);
			layoutData.left = new FormAttachment(0, 1000, 0);
			layoutData.right = new FormAttachment(458, 1000, 0);
			layoutData.bottom = new FormAttachment(1000, 1000, 0);
			new GPSLoggerVisualizationControl(this.visualizationMainComposite, layoutData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2010), 0, numMeasurements);

			this.subTabFolder1 = new CTabFolder(this.visualizationMainComposite, SWT.NONE);
			FormData subTabFolder1LData = new FormData();
			subTabFolder1LData.top = new FormAttachment(0, 1000, 0);
			subTabFolder1LData.left = new FormAttachment(460, 1000, 0);
			subTabFolder1LData.right = new FormAttachment(1000, 1000, 0);
			subTabFolder1LData.bottom = new FormAttachment(1000, 1000, 0);
			this.subTabFolder1.setLayoutData(subTabFolder1LData);

			{
				this.uniLogTabItem = new CTabItem(this.subTabFolder1, SWT.NONE);
				this.uniLogTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
				this.uniLogTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2011));
				this.uniLogVisualization = new Composite(this.subTabFolder1, SWT.NONE);
				FormLayout compositeLayout = new FormLayout();
				this.uniLogVisualization.setLayout(compositeLayout);
				this.uniLogTabItem.setControl(this.uniLogVisualization);
				FormData layoutUniLogData = new FormData();
				layoutUniLogData.top = new FormAttachment(0, 1000, 0);
				layoutUniLogData.left = new FormAttachment(0, 1000, 0);
				layoutUniLogData.right = new FormAttachment(1000, 1000, 0);
				layoutUniLogData.bottom = new FormAttachment(1000, 1000, 0);
				new GPSLoggerVisualizationControl(this.uniLogVisualization, layoutUniLogData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2011), numMeasurements, numMeasurements_UL);
			}
			{
				this.mLinkTabItem = new CTabItem(this.subTabFolder1, SWT.NONE);
				this.mLinkTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
				this.mLinkTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2012));
				this.mLinkVisualization = new Composite(this.subTabFolder1, SWT.NONE);
				FormLayout compositeLayout = new FormLayout();
				this.mLinkVisualization.setLayout(compositeLayout);
				this.mLinkTabItem.setControl(this.mLinkVisualization);
				FormData layoutMLinkData = new FormData();
				layoutMLinkData.top = new FormAttachment(0, 1000, 0);
				layoutMLinkData.left = new FormAttachment(0, 1000, 0);
				layoutMLinkData.right = new FormAttachment(1000, 1000, 0);
				layoutMLinkData.bottom = new FormAttachment(1000, 1000, 0);
				new GPSLoggerVisualizationControl(this.mLinkVisualization, layoutMLinkData, this, channelNumber, this.device, Messages.getString(MessageIds.GDE_MSGT2012), numMeasurements+numMeasurements_UL, numMeasurements_ML);
			}
			this.subTabFolder1.setSelection(0);
		}
	}

	/**
	 * set the save visualization configuration button enabled 
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveVisualizationButton.setEnabled(enable);
		this.application.updateAllTabs(true);
	}

	/**
	 * set the save configuration button enabled 
	 */
	public void enableSaveConfigurationButton(boolean enable) {
		this.saveSetupButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}
}
