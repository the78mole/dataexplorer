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
    
    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceDialog;
import gde.device.IDevice;
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
	final static Logger		log									= Logger.getLogger(GPSLoggerDialog.class.getName());

	CTabFolder						tabFolder, subTabFolder1, subTabFolder2;
	CTabItem							visualizationTabItem, configurationTabItem, uniLogTabItem, mLinkTabItem;
	Composite							visualizationMainComposite, uniLogVisualization, mLinkVisualization;
	Composite							configurationMainComposite, configuration1Composite, configuration2Composite;

	Button								saveVisualizationButton, inputFileButton, saveSetupButton, closeButton;

	CTabItem							gpsLoggerTabItem, telemetryTabItem;

	final IDevice					device;																																	// get device specific things, get serial port, ...
	final Settings				settings;																																// application configuration settings

	boolean								isVisibilityChanged	= false;
	int										measurementsCount		= 0;
	final List<CTabItem>	configurations			= new ArrayList<CTabItem>();

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public GPSLoggerDialog(Shell parent, IDevice useDevice) {
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
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				dialogShell.layout();
				dialogShell.pack();
				this.dialogShell.setSize(700, 10 + 30 + 90 + this.measurementsCount * 30 + 25);
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
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
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						GPSLoggerDialog.this.application.openHelpDialog("GPS-Logger", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					this.tabFolder.setSimple(false);
					{
						visualizationTabItem = new CTabItem(tabFolder, SWT.NONE);
						visualizationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
						visualizationTabItem.setText("Visualization Configuration");

						visualizationMainComposite = new Composite(tabFolder, SWT.NONE);
						FormLayout visualizationMainCompositeLayout = new FormLayout();
						visualizationMainComposite.setLayout(visualizationMainCompositeLayout);
						visualizationTabItem.setControl(visualizationMainComposite);
						{
							FormData layoutData = new FormData();
							layoutData.top = new FormAttachment(0, 1000, 0);
							layoutData.left = new FormAttachment(0, 1000, 0);
							layoutData.right = new FormAttachment(500, 1000, 0);
							layoutData.bottom = new FormAttachment(1000, 1000, 0);
							new GPSLoggerVisualizationControl(visualizationMainComposite, layoutData, this, 1, this.device, "GPS-Logger", 0, 15);

							subTabFolder1 = new CTabFolder(visualizationMainComposite, SWT.NONE);
							FormData subTabFolder1LData = new FormData();
							subTabFolder1LData.top = new FormAttachment(0, 1000, 0);
							subTabFolder1LData.left = new FormAttachment(500, 1000, 0);
							subTabFolder1LData.right = new FormAttachment(1000, 1000, 0);
							subTabFolder1LData.bottom = new FormAttachment(1000, 1000, 0);
							subTabFolder1.setLayoutData(subTabFolder1LData);

							{								
								uniLogTabItem = new CTabItem(subTabFolder1, SWT.NONE);
								uniLogTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
								uniLogTabItem.setText("UniLog");
								uniLogVisualization = new Composite(subTabFolder1, SWT.NONE);
								FormLayout compositeLayout = new FormLayout();
								uniLogVisualization.setLayout(compositeLayout);
								uniLogTabItem.setControl(uniLogVisualization);
								FormData layoutUniLogData = new FormData();
								layoutUniLogData.top = new FormAttachment(0, 1000, 0);
								layoutUniLogData.left = new FormAttachment(0, 1000, 0);
								layoutUniLogData.right = new FormAttachment(1000, 1000, 0);
								layoutUniLogData.bottom = new FormAttachment(1000, 1000, 0);
								new GPSLoggerVisualizationControl(uniLogVisualization, layoutUniLogData, this, 1, this.device, "UniLog", 15, 9);
							}
							{
								mLinkTabItem = new CTabItem(subTabFolder1, SWT.NONE);
								mLinkTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
								mLinkTabItem.setText("M-Link");
								mLinkVisualization = new Composite(subTabFolder1, SWT.NONE);
								FormLayout compositeLayout = new FormLayout();
								mLinkVisualization.setLayout(compositeLayout);
								mLinkTabItem.setControl(mLinkVisualization);
								FormData layoutMLinkData = new FormData();
								layoutMLinkData.top = new FormAttachment(0, 1000, 0);
								layoutMLinkData.left = new FormAttachment(0, 1000, 0);
								layoutMLinkData.right = new FormAttachment(1000, 1000, 0);
								layoutMLinkData.bottom = new FormAttachment(1000, 1000, 0);
								new GPSLoggerVisualizationControl(mLinkVisualization, layoutMLinkData, this, 1, this.device, "M-Link", 24, 15);
							}
							subTabFolder1.setSelection(0);
						}
					}
					{
						configurationTabItem = new CTabItem(tabFolder, SWT.NONE);
						configurationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
						configurationTabItem.setText("Setup Configuration");

						configurationMainComposite = new Composite(tabFolder, SWT.NONE);
						FormLayout configurationMainCompositeLayout = new FormLayout();
						configurationMainComposite.setLayout(configurationMainCompositeLayout);
						configurationTabItem.setControl(configurationMainComposite);
						{
							FormData layoutConfig1Data = new FormData();
							layoutConfig1Data.top = new FormAttachment(0, 1000, 0);
							layoutConfig1Data.left = new FormAttachment(0, 1000, 0);
							layoutConfig1Data.right = new FormAttachment(493, 1000, 0);
							layoutConfig1Data.bottom = new FormAttachment(1000, 1000, 0);
							configuration1Composite = new GPSLoggerSetupConfiguration1(configurationMainComposite, SWT.None);
							configuration1Composite.setLayoutData(layoutConfig1Data);
						}
						{
							FormData layoutConfig2Data = new FormData();
							layoutConfig2Data.top = new FormAttachment(0, 1000, 0);
							layoutConfig2Data.left = new FormAttachment(493, 1000, 0);
							layoutConfig2Data.right = new FormAttachment(1000, 1000, 0);
							layoutConfig2Data.bottom = new FormAttachment(1000, 1000, 0);
							configuration2Composite = new GPSLoggerSetupConfiguration2(configurationMainComposite, SWT.None);
							configuration2Composite.setLayoutData(layoutConfig2Data);
						}
					}
					this.tabFolder.setSelection(0);
					this.tabFolder.addListener(SWT.Selection, new Listener() {				
						@Override
						public void handleEvent(Event event) {
							if(tabFolder.getSelectionIndex() == 1)
							application.openFileOpenDialog("Open GPS-Logger Setup File", new String[] {"*.bin"}, "F:/", "setupFilename.sm", SWT.SINGLE);
							
						}
					});

				}
				{
					this.saveVisualizationButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveButtonLData = new FormData();
					saveButtonLData.width = 130;
					saveButtonLData.height = 30;
					saveButtonLData.bottom = new FormAttachment(1000, 1000, -10);
					saveButtonLData.left = new FormAttachment(0, 1000, 15);
					this.saveVisualizationButton.setLayoutData(saveButtonLData);
					this.saveVisualizationButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveVisualizationButton.setText("save visualization");
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
					inputFileButtonLData.height = 30;
					inputFileButtonLData.bottom = new FormAttachment(1000, 1000, -10);
					inputFileButtonLData.left = new FormAttachment(0, 1000, 190);
					this.inputFileButton.setLayoutData(inputFileButtonLData);
					this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.inputFileButton.setText("open input file");
					this.inputFileButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (isVisibilityChanged) {
								String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { device.getPropertiesFileName() });
								if (application.openYesNoMessageDialog(dialogShell, msg) == SWT.YES) {
									log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
									device.storeDeviceProperties();
								}
							}
							device.openCloseSerialPort();
						}
					});
				}
				{
					this.saveSetupButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData saveSetupButtonLData = new FormData();
					saveSetupButtonLData.width = 130;
					saveSetupButtonLData.height = 30;
					saveSetupButtonLData.right = new FormAttachment(1000, 1000, -190);
					saveSetupButtonLData.bottom = new FormAttachment(1000, 1000, -10);
					this.saveSetupButton.setLayoutData(saveSetupButtonLData);
					this.saveSetupButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.saveSetupButton.setText("save setup");
					this.saveSetupButton.setEnabled(false);
					this.saveSetupButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "saveSetupButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					FormData closeButtonLData = new FormData();
					closeButtonLData.width = 130;
					closeButtonLData.height = 30;
					closeButtonLData.right = new FormAttachment(1000, 1000, -15);
					closeButtonLData.bottom = new FormAttachment(1000, 1000, -10);
					this.closeButton.setLayoutData(closeButtonLData);
					this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerDialog.this.dialogShell.dispose();
						}
					});
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 10));
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
	 * implementation of noop method from base dialog class
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveVisualizationButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}
}
