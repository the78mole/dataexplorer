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
package osde.device.wb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.device.IDevice;
import osde.device.wstech.MessageIds;
import osde.log.Level;
import osde.messages.Messages;
import osde.ui.SWTResourceManager;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class CSV2SerialAdapterDialog extends DeviceDialog {
	final static Logger						log	= Logger.getLogger(CSV2SerialAdapterDialog.class.getName());
	
	CTabFolder tabFolder;
	Button closeButton;
	Button saveButton;

	final IDevice								device;																						// get device specific things, get serial port, ...
	final Settings								settings;																						// application configuration settings

	int measurementsCount = 0;
	final List<CTabItem>					configurations = new ArrayList<CTabItem>();
	
	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public CSV2SerialAdapterDialog(Shell parent, IDevice useDevice) {
		super(parent);
		this.device = useDevice;
		this.settings = Settings.getInstance();
		for (int i = 1; i <= this.device.getChannelCount(); i++) {
			int actualMeasurementCount = this.device.getMeasurementNames(i).length;
			measurementsCount = actualMeasurementCount > measurementsCount ? actualMeasurementCount : measurementsCount;
		}
	}

	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				dialogShell.setLayout(dialogShellLayout);
				dialogShell.layout();
				//dialogShell.pack();
				dialogShell.setSize(310, 10 + 30 + 90 + measurementsCount * 30 + 55);
				dialogShell.setText(device.getName() + "Dialog");
				this.dialogShell.setText(device.getName() + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt);
						if (device.isChangePropery()) {
							String msg = Messages.getString(osde.messages.MessageIds.OSDE_MSGT0469);
							if (application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
					}
				});
				{
					tabFolder = new CTabFolder(dialogShell, SWT.NONE);
					{
						for (int i = 0; i < device.getChannelCount(); i++) {
							this.configurations.add(new CSV2SerialAdapterDialogTabItem(tabFolder, this, (i + 1), device));
						}
					}
					{
						saveButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
						FormData saveButtonLData = new FormData();
						saveButtonLData.width = 120;
						saveButtonLData.height = 30;
						saveButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						//saveButtonLData.right = new FormAttachment(1000, 1000, -163);
						saveButtonLData.left = new FormAttachment(0, 1000, 15);
						saveButton.setLayoutData(saveButtonLData);
						saveButton.setText("save");
						saveButton.setEnabled(false);
						saveButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "saveButton.widgetSelected, event=" + evt);
								device.storeDeviceProperties();
								saveButton.setEnabled(false);
							}
						});
					}
					{
						closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
						FormData closeButtonLData = new FormData();
						closeButtonLData.width = 120;
						closeButtonLData.height = 30;
						//closeButtonLData.left = new FormAttachment(0, 1000, 152);
						closeButtonLData.right = new FormAttachment(1000, 1000, -15);
						closeButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						closeButton.setLayoutData(closeButtonLData);
						closeButton.setText("close");
						closeButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt);
								dialogShell.dispose();
							}
						});
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					tabFolder.setLayoutData(tabFolderLData);
					tabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = tabFolder.getSelectionIndex();
							if (channelNumber >= 0 && channelNumber <= device.getChannelCount()) { // enable other tabs for future use
								channelNumber += 1;
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels channels = Channels.getInstance();
								Channel activeChannel = channels.getActiveChannel();
								if (activeChannel != null) {
									log.log(Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null && activeChannel.getNumber() != channelNumber) {
										int answer = application.openYesNoMessageDialog(getDialogShell(), Messages.getString(MessageIds.OSDE_MSGI1801));
										if (answer == SWT.YES) {
											String recordSetKey = activeRecordSet.getName();
											Channel tmpChannel = channels.get(channelNumber);
											if (tmpChannel != null) {
												log.log(Level.FINE, "move record set " + recordSetKey + " to channel/configuration " + channelNumber + OSDE.STRING_BLANK_COLON_BLANK + configKey); //$NON-NLS-1$ //$NON-NLS-2$
												tmpChannel.put(recordSetKey, activeRecordSet.clone(channelNumber));
												activeChannel.remove(recordSetKey);
												channels.switchChannel(channelNumber, recordSetKey);
												RecordSet newActiveRecordSet = channels.get(channelNumber).getActiveRecordSet();
												if (newActiveRecordSet != null) {
													device.updateVisibilityStatus(newActiveRecordSet);
													device.makeInActiveDisplayable(newActiveRecordSet);
												}
											}
										}
										device.updateVisibilityStatus(activeRecordSet);
									}
								}
							}
						}
					});
				}
				
				try {
					tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber()-1);
				}
				catch (RuntimeException e){
					tabFolder.setSelection(0);
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
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	/**
	 * implementation of noop method from base dialog class
	 */
	public void enableSaveButton(boolean enable) {
		this.saveButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return tabFolder.getSelectionIndex();
	}
}
