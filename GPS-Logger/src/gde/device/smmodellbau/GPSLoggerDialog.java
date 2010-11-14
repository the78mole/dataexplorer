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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class GPSLoggerDialog extends DeviceDialog {
	final static Logger		log								= Logger.getLogger(GPSLoggerDialog.class.getName());

	CTabFolder						tabFolder, subTabFolder1, subTabFolder2;
	CTabItem 							visualizationTabItem, configurationTabItem;
	Button								closeButton;
	Button								saveButton;

	CTabItem 							gpsLoggerTabItem, telemetryTabItem;

	final IDevice					device;																																				// get device specific things, get serial port, ...
	final Settings				settings;																																			// application configuration settings

	int										measurementsCount	= 0;
	final List<CTabItem>	configurations		= new ArrayList<CTabItem>();

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
				this.dialogShell.layout();
				//dialogShell.pack();
				this.dialogShell.setSize(350, 10 + 30 + 90 + this.measurementsCount * 30 + 55);
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (GPSLoggerDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] {GPSLoggerDialog.this.device.getPropertiesFileName()});
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
					this.tabFolder.setSimple(false);
					{
						visualizationTabItem = new CTabItem(tabFolder, SWT.NONE);
						visualizationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						visualizationTabItem.setText("Visualization Configuration");
						{
							subTabFolder1 = new CTabFolder(tabFolder, SWT.NONE);
							subTabFolder1.setSimple(false);
							visualizationTabItem.setControl(subTabFolder1);
							{
								this.configurations.add(new GPSLoggerVisualizationTabItem(this.subTabFolder1, this, 1, this.device, "GPS-Logger", 0, 15));
								this.configurations.add(new GPSLoggerVisualizationTabItem(this.subTabFolder1, this, 1, this.device, "UniLog", 15, 9));
								this.configurations.add(new GPSLoggerVisualizationTabItem(this.subTabFolder1, this, 1, this.device, "M-Link", 24, 15));
							}
							{
								this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
								FormData saveButtonLData = new FormData();
								saveButtonLData.width = 120;
								saveButtonLData.height = 30;
								saveButtonLData.bottom = new FormAttachment(1000, 1000, -10);
								saveButtonLData.left = new FormAttachment(0, 1000, 15);
								this.saveButton.setLayoutData(saveButtonLData);
								this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
								this.saveButton.setEnabled(false);
								this.saveButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										GPSLoggerDialog.this.device.storeDeviceProperties();
										GPSLoggerDialog.this.saveButton.setEnabled(false);
									}
								});
							}
							{
								this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
								FormData closeButtonLData = new FormData();
								closeButtonLData.width = 120;
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
							FormData tabFolderLData = new FormData();
							tabFolderLData.top = new FormAttachment(0, 1000, 0);
							tabFolderLData.left = new FormAttachment(0, 1000, 0);
							tabFolderLData.right = new FormAttachment(1000, 1000, 0);
							tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
							this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.tabFolder.setLayoutData(tabFolderLData);
						}
						subTabFolder1.setSelection(0);
					}
				}
				{
					configurationTabItem = new CTabItem(tabFolder, SWT.NONE);
					configurationTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					configurationTabItem.setText("Setup Configuration");
					{
						subTabFolder2 = new CTabFolder(tabFolder, SWT.NONE);
						subTabFolder2.setSimple(false);
						configurationTabItem.setControl(subTabFolder2);
						{
							{
								gpsLoggerTabItem = new CTabItem(subTabFolder2, SWT.NONE);
								gpsLoggerTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								gpsLoggerTabItem.setText("GPS-Logger");
								gpsLoggerTabItem.setControl(new GPSLoggerSetupConfiguration1(subTabFolder2, SWT.None));
							}
							{
								telemetryTabItem = new CTabItem(subTabFolder2, SWT.NONE);
								telemetryTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								telemetryTabItem.setText("Telemetry");
								telemetryTabItem.setControl(new GPSLoggerSetupConfiguration2(subTabFolder2, SWT.None));
							}
						}
						subTabFolder2.setSelection(0);
					}
				}
				this.tabFolder.setSelection(0);

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
		this.saveButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolder.getSelectionIndex();
	}
}
