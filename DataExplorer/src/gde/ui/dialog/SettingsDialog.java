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
package osde.ui.dialog;

import java.util.Vector;
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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.config.Settings;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class to adjust application wide properties
 * @author Winfried Brügmann
 */
public class SettingsDialog extends Dialog {
	final static Logger						log										= Logger.getLogger(SettingsDialog.class.getName());
	static final String						STRING_LOG_LEVEL_INFO	= "INFO";																					//$NON-NLS-1$

	CCombo												configLevelCombo;
	CLabel												utilsLevelLabel;
	CCombo												utilsLevelCombo;
	CLabel												serialIOLevelLabel;
	CCombo												serialIOLevelCombo;
	CLabel												configLevelLabel;
	Button												okButton;
	Button												globalLogLevel;
	CLabel												commonLevelLabel;
	CCombo												commonLevelCombo;
	CLabel												deviceLevelLabel;
	CCombo												deviceLevelCombo;
	CCombo												uiLevelCombo;
	CLabel												uiLevelLabel;
	Composite											individualLoggingComosite;
	Composite											globalLoggingComposite;
	Shell													dialogShell;
	CLabel												defaultDataPathLabel;
	Group													defaultDataPathGroup;
	CLabel												Port;
	CCombo												serialPort;
	Button												useGlobalSerialPort;
	CLabel												localLabel;
	CCombo												localCombo;
	Group													groupLocale;
	Button												doPortAvailabilityCheck;
	Button												suggestObjectKey;
	Composite											tabComposite1;
	Composite											analysisComposite;
	CTabItem											generalTabItem;
	CTabItem											analysisTabItem;
	CTabFolder										cTabFolder1;
	Slider												alphaSlider;
	Button												suggestDate;
	Group													fileOpenSaveDialogGroup;
	CLabel												fileIOLevelLabel;
	CCombo												fileIOLevelCombo;
	Button												deviceDialogButton;
	Button												deviceDialogAlphaButton;
	Group													deviceDialogGroup;
	Group													serialPortGroup;
	Group													separatorGroup;
	CCombo												listSeparator;
	CLabel												listSeparatorLabel;
	CCombo												decimalSeparator;
	CLabel												decimalSeparatorLabel;
	Button												defaultDataPathAdjustButton;
	Text													defaultDataPath;
	CCombo												globalLoggingCombo;
	Group													loggingGroup;

	Thread												listPortsThread;
	Vector<String>								availablePorts				= new Vector<String>();
	final Settings								settings;
	final OpenSerialDataExplorer	application;
	final String[] 								supportedLocals				= {"en", "de"}; //$NON-NLS-1$ //$NON-NLS-2$

	public SettingsDialog(Shell parent, int style) {
		super(parent, style);
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	public void open() {
		try {
			{
			}
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(496, 527);
			this.dialogShell.setText(OSDE.OSDE_NAME_LONG + Messages.getString(MessageIds.OSDE_MSGT0300));
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.gif")); //$NON-NLS-1$
			{ // begin tab folder
				this.cTabFolder1 = new CTabFolder(this.dialogShell, SWT.NONE);
				FormData cTabFolder1LData = new FormData();
				cTabFolder1LData.width = 484;
				cTabFolder1LData.height = 419;
				cTabFolder1LData.left = new FormAttachment(0, 1000, 0);
				cTabFolder1LData.right = new FormAttachment(1000, 1000, 0);
				cTabFolder1LData.top = new FormAttachment(0, 1000, 1);
				this.cTabFolder1.setLayoutData(cTabFolder1LData);
				{ // begin general tab item
					this.generalTabItem = new CTabItem(this.cTabFolder1, SWT.NONE);
					this.generalTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0301));
					{
						FormData tabComposite1LData = new FormData();
						tabComposite1LData.width = 457;
						tabComposite1LData.height = 402;
						tabComposite1LData.left = new FormAttachment(0, 1000, 12);
						tabComposite1LData.top = new FormAttachment(0, 1000, 7);
						this.tabComposite1 = new Composite(this.cTabFolder1, SWT.NONE);
						this.generalTabItem.setControl(this.tabComposite1);
						this.tabComposite1.setLayout(new FormLayout());
						{
							FormData groupLocaleLData = new FormData();
							groupLocaleLData.width = 451;
							groupLocaleLData.height = 38;
							groupLocaleLData.left = new FormAttachment(0, 1000, 12);
							groupLocaleLData.top = new FormAttachment(0, 1000, 7);
							this.groupLocale = new Group(this.tabComposite1, SWT.NONE);
							this.groupLocale.setLayout(null);
							this.groupLocale.setLayoutData(groupLocaleLData);
							this.groupLocale.setText(Messages.getString(MessageIds.OSDE_MSGT0305));
							{
								this.localCombo = new CCombo(this.groupLocale, SWT.BORDER);
								this.localCombo.setItems(this.supportedLocals);
								this.localCombo.select(getLocalLanguageIndex());
								this.localCombo.setBounds(354, 22, 54, 20);
								this.localCombo.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0306));
								this.localCombo.setEditable(false);
								this.localCombo.setBackground(SWTResourceManager.getColor(255, 255, 255));
							}
							{
								this.localLabel = new CLabel(this.groupLocale, SWT.LEFT);
								this.localLabel.setBounds(37, 22, 292, 20);
								this.localLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0307));
								this.localLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0308));
							}
						}
						{ // begin default data path group
							this.defaultDataPathGroup = new Group(this.tabComposite1, SWT.NONE);
							this.defaultDataPathGroup.setLayout(null);
							FormData group1LData = new FormData();
							group1LData.width = 451;
							group1LData.height = 42;
							group1LData.left = new FormAttachment(0, 1000, 12);
							group1LData.top = new FormAttachment(0, 1000, 66);
							this.defaultDataPathGroup.setLayoutData(group1LData);
							this.defaultDataPathGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0310));
							this.defaultDataPathGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.finest("defaultDataPathGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.defaultDataPath.setText(SettingsDialog.this.settings.getDataFilePath());
								}
							});
							{
								this.defaultDataPathLabel = new CLabel(this.defaultDataPathGroup, SWT.NONE);
								this.defaultDataPathLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0311));
								this.defaultDataPathLabel.setBounds(14, 24, 90, 20);
							}
							{
								this.defaultDataPath = new Text(this.defaultDataPathGroup, SWT.BORDER);
								this.defaultDataPath.setBounds(107, 24, 295, 20);
							}
							{
								this.defaultDataPathAdjustButton = new Button(this.defaultDataPathGroup, SWT.PUSH | SWT.CENTER);
								this.defaultDataPathAdjustButton.setText(". . . "); //$NON-NLS-1$
								this.defaultDataPathAdjustButton.setBounds(405, 24, 30, 20);
								this.defaultDataPathAdjustButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("defaultDataPathAdjustButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										String defaultDataDirectory = SettingsDialog.this.application.openDirFileDialog(Messages.getString(MessageIds.OSDE_MSGT0312), SettingsDialog.this.settings.getDataFilePath());
										SettingsDialog.log.fine("default directory from directoy dialog = " + defaultDataDirectory); //$NON-NLS-1$
										SettingsDialog.this.settings.setDataFilePath(defaultDataDirectory);
										SettingsDialog.this.defaultDataPath.setText(defaultDataDirectory);
									}
								});
							}
						} // end default data path group
						{ // begin file save dialog filename leader
							FormData fileOpenSaveDialogGroupLData = new FormData();
							fileOpenSaveDialogGroupLData.width = 451;
							fileOpenSaveDialogGroupLData.height = 44;
							fileOpenSaveDialogGroupLData.left = new FormAttachment(0, 1000, 12);
							fileOpenSaveDialogGroupLData.top = new FormAttachment(0, 1000, 129);
							this.fileOpenSaveDialogGroup = new Group(this.tabComposite1, SWT.NONE);
							this.fileOpenSaveDialogGroup.setLayout(null);
							this.fileOpenSaveDialogGroup.setLayoutData(fileOpenSaveDialogGroupLData);
							this.fileOpenSaveDialogGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0315));
							this.fileOpenSaveDialogGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.fine("fileOpenSaveDialogGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.suggestDate.setSelection(SettingsDialog.this.settings.getUsageDateAsFileNameLeader());
									SettingsDialog.this.suggestObjectKey.setSelection(SettingsDialog.this.settings.getUsageObjectKeyInFileName());
								}
							});
							{
								this.suggestDate = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.RIGHT);
								this.suggestDate.setText(Messages.getString(MessageIds.OSDE_MSGT0316));
								this.suggestDate.setBounds(15, 28, 194, 16);
								this.suggestDate.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.fine("suggestDate.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setUsageDateAsFileNameLeader(SettingsDialog.this.suggestDate.getSelection());
									}
								});
							}
							{
								this.suggestObjectKey = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
								this.suggestObjectKey.setText(Messages.getString(MessageIds.OSDE_MSGT0317));
								this.suggestObjectKey.setEnabled(false);
								this.suggestObjectKey.setBounds(239, 28, 194, 16);
								this.suggestObjectKey.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.fine("suggestObjectKey.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setUsageObjectKeyInFileName(SettingsDialog.this.suggestObjectKey.getSelection());
									}
								});
							}
						} // end file save dialog filename leader
						{ // begin device dialog settings
							FormData deviceDialogLData = new FormData();
							deviceDialogLData.width = 451;
							deviceDialogLData.height = 55;
							deviceDialogLData.left = new FormAttachment(0, 1000, 12);
							deviceDialogLData.top = new FormAttachment(0, 1000, 194);
							this.deviceDialogGroup = new Group(this.tabComposite1, SWT.NONE);
							this.deviceDialogGroup.setLayout(null);
							this.deviceDialogGroup.setLayoutData(deviceDialogLData);
							this.deviceDialogGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0318));
							this.deviceDialogGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.finest("deviceDialogGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.deviceDialogButton.setSelection(SettingsDialog.this.settings.isDeviceDialogsModal());
									SettingsDialog.this.deviceDialogAlphaButton.setSelection(SettingsDialog.this.settings.isDeviceDialogAlphaEnabled());
									SettingsDialog.this.alphaSlider.setEnabled(SettingsDialog.this.settings.isDeviceDialogAlphaEnabled());
									SettingsDialog.this.alphaSlider.setSelection(SettingsDialog.this.settings.getDialogAlphaValue());
								}
							});
							{
								this.deviceDialogButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogButton.setText(Messages.getString(MessageIds.OSDE_MSGT0319));
								this.deviceDialogButton.setBounds(16, 24, 254, 18);
								this.deviceDialogButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0320));
								this.deviceDialogButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("deviceDialogButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.enabelModalDeviceDialogs(SettingsDialog.this.deviceDialogButton.getSelection());
									}
								});
							}
							{
								this.deviceDialogAlphaButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogAlphaButton.setText(Messages.getString(MessageIds.OSDE_MSGT0321));
								this.deviceDialogAlphaButton.setBounds(16, 47, 254, 18);
								this.deviceDialogAlphaButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0322));
								this.deviceDialogAlphaButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("deviceDialogButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDeviceDialogAlphaEnabled(SettingsDialog.this.deviceDialogAlphaButton.getSelection());
										SettingsDialog.this.alphaSlider.setEnabled(SettingsDialog.this.deviceDialogAlphaButton.getSelection());
									}
								});
							}
							{
								this.alphaSlider = new Slider(this.deviceDialogGroup, SWT.NONE);
								this.alphaSlider.setBounds(282, 47, 163, 20);
								this.alphaSlider.setIncrement(5);
								this.alphaSlider.setMinimum(10);
								this.alphaSlider.setMaximum(180);
								this.alphaSlider.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finer("alphaSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
										switch (evt.detail) {
										case SWT.DRAG:
											SettingsDialog.this.dialogShell.setAlpha(SettingsDialog.this.alphaSlider.getSelection());
											break;
										default:
										case SWT.NONE:
											SettingsDialog.this.settings.setDialogAlphaValue(SettingsDialog.this.alphaSlider.getSelection());
											SettingsDialog.this.dialogShell.setAlpha(254);
											break;
										}
									}
								});
							}
						} // end device dialog settings
						{ // begin CSV separator group
							this.separatorGroup = new Group(this.tabComposite1, SWT.NONE);
							this.separatorGroup.setLayout(null);
							FormData separatorGroupLData = new FormData();
							separatorGroupLData.width = 451;
							separatorGroupLData.height = 44;
							separatorGroupLData.left = new FormAttachment(0, 1000, 12);
							separatorGroupLData.top = new FormAttachment(0, 1000, 270);
							this.separatorGroup.setLayoutData(separatorGroupLData);
							this.separatorGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0325));
							this.separatorGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.finest("separatorGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.decimalSeparator.setText(SettingsDialog.this.settings.getDecimalSeparator() + OSDE.STRING_EMPTY);
									SettingsDialog.this.listSeparator.setText(SettingsDialog.this.settings.getListSeparator() + OSDE.STRING_EMPTY);
								}
							});
							{
								this.decimalSeparatorLabel = new CLabel(this.separatorGroup, SWT.LEFT);
								this.decimalSeparatorLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0326));
								this.decimalSeparatorLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0327));
								this.decimalSeparatorLabel.setBounds(25, 24, 125, 22);
							}
							{
								this.decimalSeparator = new CCombo(this.separatorGroup, SWT.BORDER);
								this.decimalSeparator.getFont().getFontData()[0].setStyle(SWT.BOLD);
								this.decimalSeparator.setItems(new String[] {" . ", " , "}); //$NON-NLS-1$ //$NON-NLS-2$
								this.decimalSeparator.setBounds(153, 24, 43, 20);
								this.decimalSeparator.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("decimalSeparator.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDecimalSeparator(SettingsDialog.this.decimalSeparator.getText().trim());
										SettingsDialog.this.decimalSeparator.setText(OSDE.STRING_BLANK + SettingsDialog.this.decimalSeparator.getText().trim() + OSDE.STRING_BLANK);
									}
								});
							}
							{
								this.listSeparatorLabel = new CLabel(this.separatorGroup, SWT.LEFT);
								this.listSeparatorLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0328));
								this.listSeparatorLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0329));
								this.listSeparatorLabel.setBounds(240, 24, 125, 20);
							}
							{
								this.listSeparator = new CCombo(this.separatorGroup, SWT.BORDER);
								this.listSeparator.setItems(new String[] { " , ", " ; ", " : "}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								this.listSeparator.setBounds(370, 24, 47, 20);
								this.listSeparator.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("listSeparator.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setListSeparator(SettingsDialog.this.listSeparator.getText().trim());
										SettingsDialog.this.listSeparator.setText(OSDE.STRING_BLANK + SettingsDialog.this.listSeparator.getText().trim() + OSDE.STRING_BLANK);
									}
								});
							}
						} // end CSV separator group
						{ // begin serial port group
							this.serialPortGroup = new Group(this.tabComposite1, SWT.NONE);
							this.serialPortGroup.setLayout(null);
							FormData serialPortGroupLData = new FormData();
							serialPortGroupLData.left = new FormAttachment(0, 1000, 12);
							serialPortGroupLData.top = new FormAttachment(0, 1000, 335);
							serialPortGroupLData.width = 451;
							serialPortGroupLData.height = 55;
							this.serialPortGroup.setLayoutData(serialPortGroupLData);
							this.serialPortGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0330));
							this.serialPortGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.finest("serialPortGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.doPortAvailabilityCheck.setSelection(SettingsDialog.this.settings.doPortAvailabilityCheck());
									SettingsDialog.this.useGlobalSerialPort.setSelection(SettingsDialog.this.settings.isGlobalSerialPort());
									//serialPort.setText(settings.getSerialPort());
									SettingsDialog.this.serialPort.setItems(SettingsDialog.this.availablePorts.toArray(new String[SettingsDialog.this.availablePorts.size()]));
									int index = SettingsDialog.this.availablePorts.indexOf(SettingsDialog.this.settings.getSerialPort());
									SettingsDialog.this.serialPort.select(index != -1 ? index : 0);
								}
							});
							{
								this.doPortAvailabilityCheck = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.doPortAvailabilityCheck.setBounds(15, 19, 243, 22);
								this.doPortAvailabilityCheck.setText(Messages.getString(MessageIds.OSDE_MSGT0331));
								this.doPortAvailabilityCheck.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0332));
								this.doPortAvailabilityCheck.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.fine("doPortAvailabilityCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setPortAvailabilityCheck(SettingsDialog.this.doPortAvailabilityCheck.getSelection());
									}
								});
							}
							{
								this.useGlobalSerialPort = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.useGlobalSerialPort.setText(Messages.getString(MessageIds.OSDE_MSGT0333));
								this.useGlobalSerialPort.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0334));
								this.useGlobalSerialPort.setBounds(15, 43, 243, 22);
								this.useGlobalSerialPort.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("useGlobalSerialPort.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.useGlobalSerialPort.getSelection()) {
											SettingsDialog.this.settings.setIsGlobalSerialPort("true"); //$NON-NLS-1$
											updateAvailablePorts();
										}
										else {
											SettingsDialog.this.settings.setIsGlobalSerialPort("false"); //$NON-NLS-1$
										}
									}
								});
							}
							{
								this.serialPort = new CCombo(this.serialPortGroup, SWT.BORDER);
								this.serialPort.setBounds(260, 45, 181, 20);
								this.serialPort.setEditable(false);
								this.serialPort.setBackground(SWTResourceManager.getColor(255, 255, 255));
								this.serialPort.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.finest("serialPort.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSerialPort(SettingsDialog.this.serialPort.getText());
									}
								});
							}
							{
								this.Port = new CLabel(this.serialPortGroup, SWT.LEFT);
								this.Port.setText(Messages.getString(MessageIds.OSDE_MSGT0335));
								this.Port.setBounds(264, 19, 174, 20);
							}
						} // end serial port group
					} // end tabComposite1
				} // end general tab item
				{ // begin analysis tab item
					this.analysisTabItem = new CTabItem(this.cTabFolder1, SWT.NONE);
					this.analysisTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0302));
					{ // begin analysis composite
						FormData analysisCompositeLData = new FormData();
						analysisCompositeLData.width = 451;
						analysisCompositeLData.height = 184;
						analysisCompositeLData.left = new FormAttachment(0, 1000, 12);
						analysisCompositeLData.top = new FormAttachment(0, 1000, 427);
						this.analysisComposite = new Composite(this.cTabFolder1, SWT.NONE);
						this.analysisTabItem.setControl(this.analysisComposite);
						this.analysisComposite.setLayout(null);
						this.analysisComposite.setLayoutData(analysisCompositeLData);
						{ // begin logging group
							this.loggingGroup = new Group(this.analysisComposite, SWT.NONE);
							this.loggingGroup.setLayout(null);
							this.loggingGroup.setBounds(13, 8, 456, 216);
							this.loggingGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0340));
							this.loggingGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.finest("loggingGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.globalLogLevel.setSelection(SettingsDialog.this.settings.isGlobalLogLevel());
									if (SettingsDialog.this.settings.isGlobalLogLevel()) {
										enableIndividualLogging(false);
										SettingsDialog.this.globalLoggingCombo.setEnabled(true);
									}
									else {
										enableIndividualLogging(true);
										SettingsDialog.this.globalLoggingCombo.setEnabled(false);
										SettingsDialog.this.globalLogLevel.setSelection(false);
									}
									updateLoggingLevels();
								}
							});
							{ // begin gloabal logging settings
								this.globalLoggingComposite = new Composite(this.loggingGroup, SWT.NONE);
								this.globalLoggingComposite.setLayout(null);
								this.globalLoggingComposite.setBounds(6, 19, 154, 50);
								{
									this.globalLogLevel = new Button(this.globalLoggingComposite, SWT.CHECK | SWT.LEFT);

									this.globalLogLevel.setText(Messages.getString(MessageIds.OSDE_MSGT0341));
									this.globalLogLevel.setBounds(4, 3, 148, 21);
									this.globalLogLevel.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("globalLogLevel.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (SettingsDialog.this.globalLogLevel.getSelection()) {
												enableIndividualLogging(false);
												SettingsDialog.this.globalLoggingCombo.setEnabled(true);
												SettingsDialog.this.settings.setIsGlobalLogLevel("true"); //$NON-NLS-1$
											}
											else {
												enableIndividualLogging(true);
												SettingsDialog.this.globalLoggingCombo.setEnabled(false);
												SettingsDialog.this.settings.setIsGlobalLogLevel("false"); //$NON-NLS-1$
											}

										}
									});
								}
								{
									this.globalLoggingCombo = new CCombo(this.globalLoggingComposite, SWT.BORDER);
									this.globalLoggingCombo.setItems(Settings.LOGGING_LEVEL);
									this.globalLoggingCombo.setBounds(4, 28, 148, 21);
									this.globalLoggingCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("globalLoggingCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, SettingsDialog.this.globalLoggingCombo.getText());
											SettingsDialog.this.globalLoggingCombo.setText(SettingsDialog.this.globalLoggingCombo.getText());
										}
									});
								}
							} // end gloabal logging settings
							{ // begin individual package based logging settings
								this.individualLoggingComosite = new Composite(this.loggingGroup, SWT.NONE);
								this.individualLoggingComosite.setLayout(null);
								this.individualLoggingComosite.setBounds(172, 19, 278, 178);
								{
									this.uiLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout uiLevelLabelLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

									this.uiLevelLabel.setLayout(uiLevelLabelLayout);
									this.uiLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0342));
									this.uiLevelLabel.setBounds(3, 3, 170, 20);
								}
								{
									this.uiLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.uiLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.uiLevelCombo.setBounds(183, 3, 79, 21);
									this.uiLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("uiLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.UI_LOG_LEVEL, SettingsDialog.this.uiLevelCombo.getText());
										}
									});
								}
								{
									this.deviceLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout cLabel1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

									this.deviceLevelLabel.setLayout(cLabel1Layout);
									this.deviceLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0343));
									this.deviceLevelLabel.setBounds(3, 27, 170, 20);
								}
								{
									this.deviceLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.deviceLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.deviceLevelCombo.setBounds(183, 27, 79, 21);
									this.deviceLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("deviceLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, SettingsDialog.this.deviceLevelCombo.getText());
										}
									});
								}
								{
									this.commonLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout cLabel2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

									this.commonLevelLabel.setLayout(cLabel2Layout);
									this.commonLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0344));
									this.commonLevelLabel.setBounds(3, 51, 170, 20);
								}
								{
									this.commonLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.commonLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.commonLevelCombo.setBounds(183, 51, 79, 21);
									this.commonLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("commonLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.DATA_LOG_LEVEL, SettingsDialog.this.commonLevelCombo.getText());
										}
									});
								}
								{
									this.configLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout cLabel3Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

									this.configLevelLabel.setLayout(cLabel3Layout);
									this.configLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0345));
									this.configLevelLabel.setBounds(3, 75, 170, 20);
								}
								{
									this.configLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.configLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.configLevelCombo.setBounds(183, 75, 79, 21);
									this.configLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("configLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, SettingsDialog.this.configLevelCombo.getText());
										}
									});
								}
								{
									this.utilsLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

									this.utilsLevelLabel.setLayout(cLabel4Layout);
									this.utilsLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0346));
									this.utilsLevelLabel.setBounds(3, 99, 170, 20);
								}
								{
									this.utilsLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.utilsLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.utilsLevelCombo.setBounds(183, 99, 79, 21);
									this.utilsLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("utilsLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.UTILS_LOG_LEVEL, SettingsDialog.this.utilsLevelCombo.getText());
										}
									});
								}
								{
									this.fileIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
									this.fileIOLevelLabel.setLayout(cLabel4Layout);
									this.fileIOLevelLabel.setBounds(3, 124, 170, 20);
									this.fileIOLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0347));
								}
								{
									this.fileIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.fileIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.fileIOLevelCombo.setBounds(183, 124, 79, 21);
									this.fileIOLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("fileIOLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, SettingsDialog.this.fileIOLevelCombo.getText());
										}
									});
								}
								{
									this.serialIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
									RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

									this.serialIOLevelLabel.setLayout(cLabel4Layout);
									this.serialIOLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0348));
									this.serialIOLevelLabel.setBounds(3, 149, 170, 20);
								}
								{
									this.serialIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
									this.serialIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
									this.serialIOLevelCombo.setBounds(183, 149, 79, 21);
									this.serialIOLevelCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											SettingsDialog.log.finest("serialIOLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											SettingsDialog.this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, SettingsDialog.this.serialIOLevelCombo.getText());
										}
									});
								}

							} // end individual package based logging settings
						} // end logging group
					} // end analysis composite
				} // end analysis tab item
				this.cTabFolder1.setSelection(0);
			} // end tab folder

			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					SettingsDialog.log.fine("dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					SettingsDialog.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_1.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					SettingsDialog.log.finest("dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (SettingsDialog.this.settings.getActiveDevice().startsWith(Settings.EMPTY)) SettingsDialog.this.settings.setActiveDevice(Settings.EMPTY_SIGNATURE);
					SettingsDialog.this.settings.store();
					if (SettingsDialog.this.settings.isGlobalSerialPort()) SettingsDialog.this.application.setGloabalSerialPort(SettingsDialog.this.serialPort.getText());
					// set logging levels
					SettingsDialog.this.settings.updateLogLevel();
				}
			});
			{ // begin ok button
				FormData okButtonLData = new FormData();
				okButtonLData.width = 250;
				okButtonLData.height = 25;
				okButtonLData.left = new FormAttachment(0, 1000, 116);
				okButtonLData.bottom = new FormAttachment(1000, 1000, -12);
				okButtonLData.right = new FormAttachment(1000, 1000, -122);
				this.okButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okButton.setLayoutData(okButtonLData);
				this.okButton.setText(Messages.getString(MessageIds.OSDE_MSGT0188));
				this.okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						SettingsDialog.log.finest("okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						SettingsDialog.this.dialogShell.dispose();
					}
				});
			} // end ok button
			this.dialogShell.setLocation(this.getParent().toDisplay(100, 100));
			this.dialogShell.open();

			updateAvailablePorts();

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get the index of preferences locale
	 */
	private int getLocalLanguageIndex() {
		int index = 0; // en
		String language = this.settings.getLocale().toString();
		for (; index < this.supportedLocals.length; index++) {
			if(this.supportedLocals[index].equals(language)) 
				return index;
		}
		return index;
	}

	/**
	 * 
	 */
	void updateAvailablePorts() {
		// execute independent from dialog UI
		this.listPortsThread = new Thread() {
			@Override
			public void run() {
				SettingsDialog.this.availablePorts = DeviceSerialPort.listConfiguredSerialPorts();
				if (SettingsDialog.this.availablePorts != null && SettingsDialog.this.availablePorts.size() > 0) {
					SettingsDialog.this.dialogShell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							SettingsDialog.this.serialPortGroup.redraw();
						}
					});
				}
			}
		};
		this.listPortsThread.start();
	}

	/**
	 * method to enable / disable log level group
	 */
	void enableIndividualLogging(boolean value) {
		this.uiLevelLabel.setEnabled(value);
		this.uiLevelCombo.setEnabled(value);
		this.commonLevelLabel.setEnabled(value);
		this.commonLevelCombo.setEnabled(value);
		this.deviceLevelLabel.setEnabled(value);
		this.deviceLevelCombo.setEnabled(value);
		this.configLevelLabel.setEnabled(value);
		this.configLevelCombo.setEnabled(value);
		this.utilsLevelLabel.setEnabled(value);
		this.utilsLevelCombo.setEnabled(value);
		this.fileIOLevelLabel.setEnabled(value);
		this.fileIOLevelCombo.setEnabled(value);
		this.serialIOLevelLabel.setEnabled(value);
		this.serialIOLevelCombo.setEnabled(value);
	}

	/**
	 * updates the logging levels in dialog
	 */
	void updateLoggingLevels() {
		if (this.settings.getProperty(Settings.GLOBAL_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.globalLoggingCombo.setText(this.settings.getProperty(Settings.GLOBAL_LOG_LEVEL));

		if (this.settings.getProperty(Settings.UI_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.UI_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.uiLevelCombo.setText(this.settings.getProperty(Settings.UI_LOG_LEVEL));

		if (this.settings.getProperty(Settings.DATA_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.DATA_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.commonLevelCombo.setText(this.settings.getProperty(Settings.DATA_LOG_LEVEL));

		if (this.settings.getProperty(Settings.DEVICE_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.deviceLevelCombo.setText(this.settings.getProperty(Settings.DEVICE_LOG_LEVEL));

		if (this.settings.getProperty(Settings.CONFIG_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.configLevelCombo.setText(this.settings.getProperty(Settings.CONFIG_LOG_LEVEL));

		if (this.settings.getProperty(Settings.UTILS_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.UTILS_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.utilsLevelCombo.setText(this.settings.getProperty(Settings.UTILS_LOG_LEVEL));

		if (this.settings.getProperty(Settings.FILE_IO_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.fileIOLevelCombo.setText(this.settings.getProperty(Settings.FILE_IO_LOG_LEVEL));

		if (this.settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, SettingsDialog.STRING_LOG_LEVEL_INFO);
		}
		this.serialIOLevelCombo.setText(this.settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL));
	}
}
