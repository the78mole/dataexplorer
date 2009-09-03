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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import osde.OSDE;
import osde.config.Settings;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.LogLevelSelectionContextMenu;
import osde.utils.ObjectKeyScanner;
import osde.utils.OperatingSystemHelper;
import osde.utils.StringHelper;

/**
 * Dialog class to adjust application wide properties
 * @author Winfried Brügmann
 */
public class SettingsDialog extends Dialog {
	final static Logger									log											= Logger.getLogger(SettingsDialog.class.getName());
	static final String									STRING_LOG_LEVEL_INFO		= "INFO";																					//$NON-NLS-1$

	public static final String					LOGGER_NAME							= "logger_name";																		//$NON-NLS-1$
	public static final String					LOG_LEVEL								= "log_level";																			//$NON-NLS-1$

	CCombo															configLevelCombo;
	CLabel															utilsLevelLabel;
	CCombo															utilsLevelCombo;
	CLabel															serialIOLevelLabel;
	CCombo															serialIOLevelCombo;
	CLabel															configLevelLabel;
	Button															okButton;
	Button															globalLogLevel;
	CLabel															commonLevelLabel;
	CCombo															commonLevelCombo;
	CLabel															deviceLevelLabel;
	CCombo															deviceLevelCombo;
	CCombo															uiLevelCombo;
	CLabel															uiLevelLabel;
	Composite														individualLoggingComosite;
	Composite														globalLoggingComposite;
	Shell																dialogShell;
	CLabel															defaultDataPathLabel;
	Group																defaultDataPathGroup;
	CLabel															Port;
	CCombo															serialPort;
	Button															useGlobalSerialPort;
	CLabel															localLabel;
	CCombo															localCombo;
	Group																groupLocale;
	Button															doPortAvailabilityCheck;
	Button															enableBlackListButton, enableWhiteListButton;
	Text																serialPortBlackList, serialPortWhiteList;
	Button															suggestObjectKey;
	Composite														tabComposite1;
	Composite														analysisComposite;
	CTabItem														generalTabItem;
	CTabItem														analysisTabItem;
	CTabFolder													settingsTabFolder;
	Slider															alphaSlider;
	Button															suggestDate;
	Group																fileOpenSaveDialogGroup;
	Group																objectKeyGroup;
	Button															scanObjectKeysButton;
	Button															removeMimeAssocButton;
	Group																miscDiagGroup;
	Button															resourceConsumptionButton;
	Button															assocMimeTypeButton;
	Button															removeLauncherButton;
	Button															createLauncerButton;
	Composite														osMiscComposite;
	Group																shellMimeType;
	Group																desktopLauncher;
	CTabItem														osMiscTabItem;
	CLabel															fileIOLevelLabel;
	CCombo															fileIOLevelCombo;
	Button															deviceDialogModalButton;
	Button															deviceDialogOnTopButton;
	Button															deviceDialogAlphaButton;
	Group																deviceDialogGroup;
	Group																serialPortGroup;
	Group																separatorGroup;
	CCombo															listSeparator;
	CLabel															listSeparatorLabel;
	CCombo															decimalSeparator;
	CLabel															decimalSeparatorLabel;
	Button															defaultDataPathAdjustButton;
	Text																defaultDataPath;
	CCombo															globalLoggingCombo;
	Group																loggingGroup;
	Group																classSelectionGroup;
	Label																classBasedLabel;
	Tree																tree;

	Thread															listPortsThread;
	Vector<String>											availablePorts = new Vector<String>();
	final Settings											settings;
	final OpenSerialDataExplorer				application;
	final String[]											supportedLocals					= { "en", "de" };																	//$NON-NLS-1$ //$NON-NLS-2$
	boolean															isLocaleLanguageChanged	= false;

	final LogLevelSelectionContextMenu	logLevelMenu						= new LogLevelSelectionContextMenu();
	Menu																popupmenu;

	public SettingsDialog(Shell parent, int style) {
		super(parent, style);
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	public void open() {
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(496, 560);
			this.dialogShell.setText(OSDE.OSDE_NAME_LONG + Messages.getString(MessageIds.OSDE_MSGT0300));
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.gif")); //$NON-NLS-1$
			{ // begin tab folder
				this.settingsTabFolder = new CTabFolder(this.dialogShell, SWT.FLAT | SWT.BORDER);
				this.settingsTabFolder.setSimple(false);
				FormData cTabFolder1LData = new FormData();
				cTabFolder1LData.width = 486;
				cTabFolder1LData.height = 454;
				cTabFolder1LData.left = new FormAttachment(0, 1000, 0);
				cTabFolder1LData.right = new FormAttachment(1000, 1000, 0);
				cTabFolder1LData.top = new FormAttachment(0, 1000, 0);
				this.settingsTabFolder.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
				this.settingsTabFolder.setLayoutData(cTabFolder1LData);
				{ // begin general tab item
					this.generalTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.generalTabItem.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
					this.generalTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0301));
					{
						FormData tabComposite1LData = new FormData();
						tabComposite1LData.width = 457;
						tabComposite1LData.height = 402;
						tabComposite1LData.left = new FormAttachment(0, 1000, 12);
						tabComposite1LData.top = new FormAttachment(0, 1000, 7);
						this.tabComposite1 = new Composite(this.settingsTabFolder, SWT.NONE);
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
							this.groupLocale.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.groupLocale.setLayoutData(groupLocaleLData);
							this.groupLocale.setText(Messages.getString(MessageIds.OSDE_MSGT0305));
							{
								this.localCombo = new CCombo(this.groupLocale, SWT.BORDER);
								this.localCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.localCombo.setItems(this.supportedLocals);
								this.localCombo.select(getLocalLanguageIndex());
								this.localCombo.setBounds(354, 22, 54, OSDE.IS_WINDOWS ? 21 : 25);
								this.localCombo.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0306));
								this.localCombo.setEditable(false);
								this.localCombo.setBackground(SWTResourceManager.getColor(255, 255, 255));
								this.localCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "localCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										String newLanguage = SettingsDialog.this.supportedLocals[SettingsDialog.this.localCombo.getSelectionIndex()];
										SettingsDialog.this.isLocaleLanguageChanged = !SettingsDialog.this.settings.getLocale().getLanguage().equals(newLanguage);
										SettingsDialog.this.settings.setLocaleLanguage(newLanguage);
									}
								});
							}
							{
								this.localLabel = new CLabel(this.groupLocale, SWT.LEFT);
								this.localLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.localLabel.setBounds(15, 22, 292, 20);
								this.localLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0307));
								this.localLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0308));
							}
						}
						{ // begin default data path group
							this.defaultDataPathGroup = new Group(this.tabComposite1, SWT.NONE);
							this.defaultDataPathGroup.setLayout(null);
							FormData classSelectionGroupLData = new FormData();
							classSelectionGroupLData.width = 451;
							classSelectionGroupLData.height = 42;
							classSelectionGroupLData.left = new FormAttachment(0, 1000, 12);
							classSelectionGroupLData.top = new FormAttachment(0, 1000, 66);
							this.defaultDataPathGroup.setLayoutData(classSelectionGroupLData);
							this.defaultDataPathGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.defaultDataPathGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0310));
							this.defaultDataPathGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "defaultDataPathGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.defaultDataPath.setText(SettingsDialog.this.settings.getDataFilePath());
								}
							});
							{
								this.defaultDataPathLabel = new CLabel(this.defaultDataPathGroup, SWT.NONE);
								this.defaultDataPathLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.defaultDataPathLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0311));
								this.defaultDataPathLabel.setBounds(15, 24, 90, 20);
							}
							{
								this.defaultDataPath = new Text(this.defaultDataPathGroup, SWT.BORDER);
								this.defaultDataPath.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.defaultDataPath.setBounds(107, 24, 295, OSDE.IS_WINDOWS ? 21 : 25);
							}
							{
								this.defaultDataPathAdjustButton = new Button(this.defaultDataPathGroup, SWT.PUSH | SWT.CENTER);
								this.defaultDataPathAdjustButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.defaultDataPathAdjustButton.setText(". . . "); //$NON-NLS-1$
								this.defaultDataPathAdjustButton.setBounds(405, 24, 30, 20);
								this.defaultDataPathAdjustButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "defaultDataPathAdjustButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										String defaultDataDirectory = SettingsDialog.this.application.openDirFileDialog(Messages.getString(MessageIds.OSDE_MSGT0312), SettingsDialog.this.settings.getDataFilePath());
										SettingsDialog.log.log(Level.FINE, "default directory from directoy dialog = " + defaultDataDirectory); //$NON-NLS-1$
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
							this.fileOpenSaveDialogGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.fileOpenSaveDialogGroup.setLayout(null);
							this.fileOpenSaveDialogGroup.setLayoutData(fileOpenSaveDialogGroupLData);
							this.fileOpenSaveDialogGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0315));
							this.fileOpenSaveDialogGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.log(Level.FINE, "fileOpenSaveDialogGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.suggestDate.setSelection(SettingsDialog.this.settings.getUsageDateAsFileNameLeader());
									SettingsDialog.this.suggestObjectKey.setSelection(SettingsDialog.this.settings.getUsageObjectKeyInFileName());
								}
							});
							{
								this.suggestDate = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
								this.suggestDate.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.suggestDate.setText(Messages.getString(MessageIds.OSDE_MSGT0316));
								this.suggestDate.setBounds(15, 28, 194, 16);
								this.suggestDate.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINE, "suggestDate.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setUsageDateAsFileNameLeader(SettingsDialog.this.suggestDate.getSelection());
									}
								});
							}
							{
								this.suggestObjectKey = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
								this.suggestObjectKey.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.suggestObjectKey.setText(Messages.getString(MessageIds.OSDE_MSGT0317));
								this.suggestObjectKey.setBounds(239, 28, 194, 16);
								this.suggestObjectKey.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINE, "suggestObjectKey.widgetSelected, event=" + evt); //$NON-NLS-1$
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
							this.deviceDialogGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.deviceDialogGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0318));
							this.deviceDialogGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "deviceDialogGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.deviceDialogModalButton.setSelection(SettingsDialog.this.settings.isDeviceDialogsModal());
									SettingsDialog.this.deviceDialogOnTopButton.setEnabled(!SettingsDialog.this.settings.isDeviceDialogsModal());
									SettingsDialog.this.deviceDialogOnTopButton.setSelection(SettingsDialog.this.settings.isDeviceDialogsOnTop());
									SettingsDialog.this.deviceDialogAlphaButton.setSelection(SettingsDialog.this.settings.isDeviceDialogAlphaEnabled());
									SettingsDialog.this.alphaSlider.setEnabled(SettingsDialog.this.settings.isDeviceDialogAlphaEnabled());
									SettingsDialog.this.alphaSlider.setSelection(SettingsDialog.this.settings.getDialogAlphaValue());
								}
							});
							{
								this.deviceDialogModalButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogModalButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.deviceDialogModalButton.setText(Messages.getString(MessageIds.OSDE_MSGT0319));
								this.deviceDialogModalButton.setBounds(16, 24, 254, 18);
								this.deviceDialogModalButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0320));
								this.deviceDialogModalButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogModalButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.enabelModalDeviceDialogs(SettingsDialog.this.deviceDialogModalButton.getSelection());
										SettingsDialog.this.deviceDialogOnTopButton.setEnabled(!SettingsDialog.this.deviceDialogModalButton.getSelection());
									}
								});
							}
							{
								this.deviceDialogOnTopButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogOnTopButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.deviceDialogOnTopButton.setText(Messages.getString(MessageIds.OSDE_MSGT0309));
								this.deviceDialogOnTopButton.setBounds(282, 24, 165, 18);
								this.deviceDialogOnTopButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0320));
								this.deviceDialogOnTopButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogOnTopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.enabelDeviceDialogsOnTop(SettingsDialog.this.deviceDialogOnTopButton.getSelection());
									}
								});
							}
							{
								this.deviceDialogAlphaButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
								this.deviceDialogAlphaButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.deviceDialogAlphaButton.setText(Messages.getString(MessageIds.OSDE_MSGT0321));
								this.deviceDialogAlphaButton.setBounds(16, 47, 254, 18);
								this.deviceDialogAlphaButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0322));
								this.deviceDialogAlphaButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceDialogButton.widgetSelected, event=" + evt); //$NON-NLS-1$
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
										SettingsDialog.log.log(Level.FINER, "alphaSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
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
										SettingsDialog.this.settings.setDialogAlphaValue(SettingsDialog.this.alphaSlider.getSelection());
									}
								});
							}
						} // end device dialog settings
						{ // begin CSV separator group
							this.separatorGroup = new Group(this.tabComposite1, SWT.NONE);
							this.separatorGroup.setLayout(null);
							this.separatorGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							FormData separatorGroupLData = new FormData();
							separatorGroupLData.width = 451;
							separatorGroupLData.height = 44;
							separatorGroupLData.left = new FormAttachment(0, 1000, 12);
							separatorGroupLData.top = new FormAttachment(0, 1000, 270);
							this.separatorGroup.setLayoutData(separatorGroupLData);
							this.separatorGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0325));
							this.separatorGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "separatorGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.decimalSeparator.setText(SettingsDialog.this.settings.getDecimalSeparator() + OSDE.STRING_EMPTY);
									SettingsDialog.this.listSeparator.setText(SettingsDialog.this.settings.getListSeparator() + OSDE.STRING_EMPTY);
								}
							});
							{
								this.decimalSeparatorLabel = new CLabel(this.separatorGroup, SWT.RIGHT);
								this.decimalSeparatorLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.decimalSeparatorLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0326));
								this.decimalSeparatorLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0327));
								this.decimalSeparatorLabel.setBounds(10, 24, 140, 22);
							}
							{
								this.decimalSeparator = new CCombo(this.separatorGroup, SWT.BORDER | SWT.CENTER);
								this.decimalSeparator.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.BOLD));
								this.decimalSeparator.setItems(new String[] { " . ", " , " }); //$NON-NLS-1$ //$NON-NLS-2$
								this.decimalSeparator.setBounds(153, 24, 43, OSDE.IS_WINDOWS ? 21 : 25);
								this.decimalSeparator.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "decimalSeparator.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setDecimalSeparator(SettingsDialog.this.decimalSeparator.getText().trim());
										SettingsDialog.this.decimalSeparator.setText(OSDE.STRING_BLANK + SettingsDialog.this.decimalSeparator.getText().trim() + OSDE.STRING_BLANK);
									}
								});
							}
							{
								this.listSeparatorLabel = new CLabel(this.separatorGroup, SWT.RIGHT);
								this.listSeparatorLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.listSeparatorLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0328));
								this.listSeparatorLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0329));
								this.listSeparatorLabel.setBounds(228, 24, 140, 20);
							}
							{
								this.listSeparator = new CCombo(this.separatorGroup, SWT.BORDER | SWT.CENTER);
								this.listSeparator.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.BOLD));
								this.listSeparator.setItems(new String[] { " , ", " ; ", " : " }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								this.listSeparator.setBounds(370, 24, 47, OSDE.IS_WINDOWS ? 21 : 25);
								this.listSeparator.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "listSeparator.widgetSelected, event=" + evt); //$NON-NLS-1$
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
							serialPortGroupLData.height = 93;
							this.serialPortGroup.setLayoutData(serialPortGroupLData);
							this.serialPortGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.serialPortGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0330));
							this.serialPortGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "serialPortGroup.paintControl, event=" + evt); //$NON-NLS-1$
									SettingsDialog.this.doPortAvailabilityCheck.setSelection(SettingsDialog.this.settings.doPortAvailabilityCheck());
									SettingsDialog.this.useGlobalSerialPort.setSelection(SettingsDialog.this.settings.isGlobalSerialPort());

									SettingsDialog.this.serialPortBlackList.setText(OSDE.STRING_BLANK + SettingsDialog.this.settings.getSerialPortBlackList());
									boolean isBlacklistEnabled = SettingsDialog.this.settings.isSerialPortBlackListEnabled();
									SettingsDialog.this.enableBlackListButton.setSelection(isBlacklistEnabled);
									SettingsDialog.this.serialPortBlackList.setEditable(isBlacklistEnabled);
									SettingsDialog.this.serialPortBlackList.setEnabled(isBlacklistEnabled);

									SettingsDialog.this.serialPortWhiteList.setText(OSDE.STRING_BLANK + SettingsDialog.this.settings.getSerialPortWhiteListString());
									boolean isWhitelistEnabled = SettingsDialog.this.settings.isSerialPortWhiteListEnabled();
									SettingsDialog.this.enableWhiteListButton.setSelection(isWhitelistEnabled);
									SettingsDialog.this.serialPortWhiteList.setEditable(isWhitelistEnabled);
									SettingsDialog.this.serialPortWhiteList.setEnabled(isWhitelistEnabled);
								}
							});
							{
								this.doPortAvailabilityCheck = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.doPortAvailabilityCheck.setBounds(15, 15, 243, 22);
								this.doPortAvailabilityCheck.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.doPortAvailabilityCheck.setText(Messages.getString(MessageIds.OSDE_MSGT0331));
								this.doPortAvailabilityCheck.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0332));
								this.doPortAvailabilityCheck.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINE, "doPortAvailabilityCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setPortAvailabilityCheck(SettingsDialog.this.doPortAvailabilityCheck.getSelection());
										if (SettingsDialog.this.doPortAvailabilityCheck.getSelection()) {
											SettingsDialog.this.application.openMessageDialog(SettingsDialog.this.dialogShell, Messages.getString(MessageIds.OSDE_MSGI0036));
										}
									}
								});
							}
							{
								this.useGlobalSerialPort = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.useGlobalSerialPort.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.useGlobalSerialPort.setText(Messages.getString(MessageIds.OSDE_MSGT0333));
								this.useGlobalSerialPort.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0334));
								this.useGlobalSerialPort.setBounds(15, 37, 243, 22);
								this.useGlobalSerialPort.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "useGlobalSerialPort.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.useGlobalSerialPort.getSelection()) {
											SettingsDialog.this.settings.setIsGlobalSerialPort("true"); //$NON-NLS-1$
										}
										else {
											SettingsDialog.this.settings.setIsGlobalSerialPort("false"); //$NON-NLS-1$
										}
									}
								});
							}
							{
								this.serialPort = new CCombo(this.serialPortGroup, SWT.BORDER);
								this.serialPort.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.serialPort.setBounds(260, 39, 181, 21);
								this.serialPort.setText(Messages.getString(MessageIds.OSDE_MSGT0199));
								this.serialPort.setEditable(false);
								this.serialPort.setBackground(SWTResourceManager.getColor(255, 255, 255));
								this.serialPort.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialPort.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setSerialPort(SettingsDialog.this.serialPort.getText().trim());
									}
								});
							}
							{
								this.Port = new CLabel(this.serialPortGroup, SWT.LEFT);
								this.Port.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.Port.setText(Messages.getString(MessageIds.OSDE_MSGT0335));
								this.Port.setBounds(264, 15, 174, 20);
							}
							{
								this.enableBlackListButton = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.enableBlackListButton.setText(Messages.getString(MessageIds.OSDE_MSGT0336));
								this.enableBlackListButton.setBounds(15, 59, 243, 22);
								this.enableBlackListButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.enableBlackListButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0337));
								this.enableBlackListButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "enableBlackListButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.enableBlackListButton.getSelection()) {
											SettingsDialog.this.settings.setSerialPortBlackListEnabled(true);
											SettingsDialog.this.serialPortBlackList.setEditable(true);
											SettingsDialog.this.serialPortBlackList.setEnabled(true);
											SettingsDialog.this.settings.setSerialPortWhiteListEnabled(false);
											SettingsDialog.this.enableWhiteListButton.setSelection(false);
											SettingsDialog.this.serialPortWhiteList.setEditable(false);
											SettingsDialog.this.serialPortWhiteList.setEnabled(false);
										}
										else {
											SettingsDialog.this.settings.setSerialPortBlackListEnabled(false);
											SettingsDialog.this.serialPortBlackList.setEditable(false);
											SettingsDialog.this.serialPortBlackList.setEnabled(false);
										}
									}
								});
							}
							{
								this.serialPortBlackList = new Text(this.serialPortGroup, SWT.BORDER);
								this.serialPortBlackList.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.serialPortBlackList.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0337));
								this.serialPortBlackList.setBounds(260, 61, 181, 22);
								this.serialPortBlackList.addVerifyListener(new VerifyListener() {
									@Override
									public void verifyText(VerifyEvent e) {
										log.log(Level.FINER, ""+StringHelper.verifyPortInput(e.text));
										e.doit = StringHelper.verifyPortInput(e.text);
									}
								});
								this.serialPortBlackList.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialPortBlackList.keyReleased, event=" + evt);
										if (evt.character == ' ') {
											SettingsDialog.this.settings.setSerialPortBlackList(SettingsDialog.this.serialPortBlackList.getText());
										}
										else if (evt.character == SWT.CR) {
											SettingsDialog.this.settings.setSerialPortBlackList(SettingsDialog.this.serialPortBlackList.getText());
											SettingsDialog.this.serialPortBlackList.setText(OSDE.STRING_BLANK + SettingsDialog.this.settings.getSerialPortBlackList());
										}
									}
								});
							}
							{
								this.enableWhiteListButton = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
								this.enableWhiteListButton.setText(Messages.getString(MessageIds.OSDE_MSGT0338));
								this.enableWhiteListButton.setBounds(15, 80, 243, 22);
								this.enableWhiteListButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.enableWhiteListButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0339));
								this.enableWhiteListButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "enableWhiteListButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (SettingsDialog.this.enableWhiteListButton.getSelection()) {
											SettingsDialog.this.settings.setSerialPortWhiteListEnabled(true);
											SettingsDialog.this.serialPortWhiteList.setEditable(true);
											SettingsDialog.this.serialPortWhiteList.setEnabled(true);
											SettingsDialog.this.settings.setSerialPortBlackListEnabled(false);
											SettingsDialog.this.enableBlackListButton.setSelection(false);
											SettingsDialog.this.serialPortBlackList.setEditable(false);
											SettingsDialog.this.serialPortBlackList.setEnabled(false);
										}
										else {
											SettingsDialog.this.settings.setSerialPortWhiteListEnabled(false);
											SettingsDialog.this.serialPortWhiteList.setEditable(false);
											SettingsDialog.this.serialPortWhiteList.setEnabled(false);
										}
									}
								});
							}
							{
								this.serialPortWhiteList = new Text(this.serialPortGroup, SWT.BORDER);
								this.serialPortWhiteList.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.serialPortWhiteList.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0339));
								this.serialPortWhiteList.setBounds(260, 84, 181, 22);
								this.serialPortWhiteList.addVerifyListener(new VerifyListener() {
									@Override
									public void verifyText(VerifyEvent e) {
										log.log(Level.FINER, ""+StringHelper.verifyPortInput(e.text));
										e.doit = StringHelper.verifyPortInput(e.text);
									}
								});
								this.serialPortWhiteList.addKeyListener(new KeyAdapter() {
									@Override
									public void keyReleased(KeyEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialPortWhiteList.keyReleased, event=" + evt);
										if (evt.character == ' ') {
											SettingsDialog.this.settings.setSerialPortWhiteList(SettingsDialog.this.serialPortWhiteList.getText());
										}
										else if (evt.character == SWT.CR) {
											SettingsDialog.this.settings.setSerialPortWhiteList(SettingsDialog.this.serialPortWhiteList.getText());
											SettingsDialog.this.serialPortWhiteList.setText(OSDE.STRING_BLANK + SettingsDialog.this.settings.getSerialPortWhiteListString());
										}
									}
								});
							}
						} // end serial port group
					} // end tabComposite1
				} // end general tab item
				{
					this.osMiscTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.osMiscTabItem.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
					this.osMiscTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0303));
					{
						this.osMiscComposite = new Composite(this.settingsTabFolder, SWT.NONE);
						this.osMiscTabItem.setControl(this.osMiscComposite);
						FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
						this.osMiscComposite.setLayout(composite1Layout);
						{
							this.desktopLauncher = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout desktopLauncherLayout = new RowLayout(SWT.HORIZONTAL);
							desktopLauncherLayout.center = true;
							desktopLauncherLayout.marginTop = 30;
							desktopLauncherLayout.marginWidth = 40;
							desktopLauncherLayout.spacing = 40;
							this.desktopLauncher.setLayout(desktopLauncherLayout);
							this.desktopLauncher.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.desktopLauncher.setText(Messages.getString(MessageIds.OSDE_MSGT0362));
							{
								this.createLauncerButton = new Button(this.desktopLauncher, SWT.PUSH | SWT.CENTER);
								this.createLauncerButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.createLauncerButton.setText(Messages.getString(MessageIds.OSDE_MSGT0363));
								RowData createLauncerButtonLData = new RowData();
								createLauncerButtonLData.width = 180;
								createLauncerButtonLData.height = 30;
								this.createLauncerButton.setLayoutData(createLauncerButtonLData);
								this.createLauncerButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "createLauncerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.createDesktopLink();
									}
								});
							}
							{
								this.removeLauncherButton = new Button(this.desktopLauncher, SWT.PUSH | SWT.CENTER);
								this.removeLauncherButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.removeLauncherButton.setText(Messages.getString(MessageIds.OSDE_MSGT0364));
								RowData removeLauncherButtonLData = new RowData();
								removeLauncherButtonLData.width = 180;
								removeLauncherButtonLData.height = 30;
								this.removeLauncherButton.setLayoutData(removeLauncherButtonLData);
								this.removeLauncherButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "removeLauncherButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.removeDesktopLink();
									}
								});
							}
						}
						{
							this.shellMimeType = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout shellMimeTypeLayout = new RowLayout(SWT.HORIZONTAL);
							shellMimeTypeLayout.center = true;
							shellMimeTypeLayout.marginTop = 30;
							shellMimeTypeLayout.marginWidth = 40;
							shellMimeTypeLayout.spacing = 40;
							this.shellMimeType.setLayout(shellMimeTypeLayout);
							this.shellMimeType.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.shellMimeType.setText(Messages.getString(MessageIds.OSDE_MSGT0365));
							{
								this.assocMimeTypeButton = new Button(this.shellMimeType, SWT.PUSH | SWT.CENTER);
								this.assocMimeTypeButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.assocMimeTypeButton.setText(Messages.getString(MessageIds.OSDE_MSGT0366));
								RowData assocMimeTypeButtonLData = new RowData();
								assocMimeTypeButtonLData.width = 180;
								assocMimeTypeButtonLData.height = 30;
								this.assocMimeTypeButton.setLayoutData(assocMimeTypeButtonLData);
								this.assocMimeTypeButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "assocMimeTypeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.registerApplication();
									}
								});
							}
							{
								this.removeMimeAssocButton = new Button(this.shellMimeType, SWT.PUSH | SWT.CENTER);
								this.removeMimeAssocButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.removeMimeAssocButton.setText(Messages.getString(MessageIds.OSDE_MSGT0367));
								RowData removeMimeAssocButtonLData = new RowData();
								removeMimeAssocButtonLData.width = 180;
								removeMimeAssocButtonLData.height = 30;
								this.removeMimeAssocButton.setLayoutData(removeMimeAssocButtonLData);
								this.removeMimeAssocButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "removeMimeAssocButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										OperatingSystemHelper.deregisterApplication();
									}
								});
							}
						}
						{
							this.objectKeyGroup = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout objectKeyGroupLayout = new RowLayout(SWT.HORIZONTAL);
							objectKeyGroupLayout.center = true;
							objectKeyGroupLayout.marginTop = 30;
							objectKeyGroupLayout.marginWidth = 90;
							objectKeyGroupLayout.spacing = 40;
							this.objectKeyGroup.setLayout(objectKeyGroupLayout);
							this.objectKeyGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.objectKeyGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0206));
							{
								this.scanObjectKeysButton = new Button(this.objectKeyGroup, SWT.PUSH | SWT.CENTER);
								this.scanObjectKeysButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.scanObjectKeysButton.setText(Messages.getString(MessageIds.OSDE_MSGT0207));
								this.scanObjectKeysButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0208, new Object[] { this.settings.getDataFilePath() }));
								RowData scanObjectKeysButtonLData = new RowData();
								scanObjectKeysButtonLData.width = 300;
								scanObjectKeysButtonLData.height = 30;
								this.scanObjectKeysButton.setLayoutData(scanObjectKeysButtonLData);
								this.scanObjectKeysButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "scanObjectKeysButton.widgetSelected, event=" + evt);
										final ObjectKeyScanner objLnkSearch = new ObjectKeyScanner();
										objLnkSearch.setSearchForKeys(true);
										objLnkSearch.start();
										new Thread() {
											@Override
											public void run() {
												while (objLnkSearch.isAlive()) {
													try {
														Thread.sleep(1000);
													}
													catch (InterruptedException e) {
														// ignore
													}
												}
												SettingsDialog.this.application.setObjectList(objLnkSearch.getObjectList());
												if (getParent().isDisposed())
													SettingsDialog.this.application.openMessageDialogAsync(Messages.getString(MessageIds.OSDE_MSGI0034));
												else
													SettingsDialog.this.application.openMessageDialogAsync(getParent(), Messages.getString(MessageIds.OSDE_MSGI0034));
											}
										}.start();
									}
								});
							}
						}
						{
							this.miscDiagGroup = new Group(this.osMiscComposite, SWT.NONE);
							RowLayout miscDiagGroupLayout = new RowLayout(SWT.HORIZONTAL);
							miscDiagGroupLayout.center = true;
							miscDiagGroupLayout.marginTop = 30;
							miscDiagGroupLayout.marginWidth = 90;
							miscDiagGroupLayout.spacing = 40;
							this.miscDiagGroup.setLayout(miscDiagGroupLayout);
							this.miscDiagGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.miscDiagGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0209));
							{
								this.resourceConsumptionButton = new Button(this.miscDiagGroup, SWT.PUSH | SWT.CENTER);
								this.resourceConsumptionButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.resourceConsumptionButton.setText(Messages.getString(MessageIds.OSDE_MSGT0210));
								this.resourceConsumptionButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0211));
								RowData resourceConsumptionButtonLData = new RowData();
								resourceConsumptionButtonLData.width = 300;
								resourceConsumptionButtonLData.height = 30;
								this.resourceConsumptionButton.setLayoutData(resourceConsumptionButtonLData);
								this.resourceConsumptionButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "resourceConsumptionButton.widgetSelected, event=" + evt);
										SWTResourceManager.listResourceStatus();
									}
								});
							}
						}
					}
				}
				{ // begin analysis tab item
					this.analysisTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
					this.analysisTabItem.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
					this.analysisTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0302));
					{ // begin logging group
						this.loggingGroup = new Group(this.settingsTabFolder, SWT.NONE);
						this.loggingGroup.setLayout(null);
						this.loggingGroup.setBounds(13, 8, 456, 399);
						this.loggingGroup.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
						this.loggingGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0340));
						this.analysisTabItem.setControl(this.loggingGroup);
						this.loggingGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								SettingsDialog.log.log(Level.FINEST, "loggingGroup.paintControl, event=" + evt); //$NON-NLS-1$
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
							this.globalLoggingComposite.setBounds(12, 26, 466, 41);
							{
								this.globalLogLevel = new Button(this.globalLoggingComposite, SWT.CHECK | SWT.LEFT);
								this.globalLogLevel.setBounds(12, 12, 190, 22);
								this.globalLogLevel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.globalLogLevel.setText(Messages.getString(MessageIds.OSDE_MSGT0341));
								this.globalLogLevel.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "globalLogLevel.widgetSelected, event=" + evt); //$NON-NLS-1$
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
								this.globalLoggingCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.globalLoggingCombo.setBounds(214, 12, 212, OSDE.IS_WINDOWS ? 21 : 25);
								this.globalLoggingCombo.setItems(Settings.LOGGING_LEVEL);
								this.globalLoggingCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "globalLoggingCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, SettingsDialog.this.globalLoggingCombo.getText());
										SettingsDialog.this.globalLoggingCombo.setText(SettingsDialog.this.globalLoggingCombo.getText());
									}
								});
							}
						} // end gloabal logging settings
						{ // begin individual package based logging settings
							this.individualLoggingComosite = new Composite(this.loggingGroup, SWT.NONE);
							this.individualLoggingComosite.setLayout(null);
							this.individualLoggingComosite.setBounds(11, 90, 263, 317);
							{
								this.uiLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout uiLevelLabelLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.uiLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.uiLevelLabel.setLayout(uiLevelLabelLayout);
								this.uiLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0342));
								this.uiLevelLabel.setBounds(3, 3, 170, 20);
							}
							{
								this.uiLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.uiLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.uiLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.uiLevelCombo.setBounds(183, 3, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.uiLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "uiLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.UI_LOG_LEVEL, SettingsDialog.this.uiLevelCombo.getText());
									}
								});
							}
							{
								this.deviceLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.deviceLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.deviceLevelLabel.setLayout(cLabel1Layout);
								this.deviceLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0343));
								this.deviceLevelLabel.setBounds(3, 27, 170, 20);
							}
							{
								this.deviceLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.deviceLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.deviceLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.deviceLevelCombo.setBounds(183, 27, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.deviceLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "deviceLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, SettingsDialog.this.deviceLevelCombo.getText());
									}
								});
							}
							{
								this.commonLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.commonLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.commonLevelLabel.setLayout(cLabel2Layout);
								this.commonLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0344));
								this.commonLevelLabel.setBounds(3, 51, 170, 20);
							}
							{
								this.commonLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.commonLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.commonLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.commonLevelCombo.setBounds(183, 51, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.commonLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "commonLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.DATA_LOG_LEVEL, SettingsDialog.this.commonLevelCombo.getText());
									}
								});
							}
							{
								this.configLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel3Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.configLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.configLevelLabel.setLayout(cLabel3Layout);
								this.configLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0345));
								this.configLevelLabel.setBounds(3, 75, 170, 20);
							}
							{
								this.configLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.configLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.configLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.configLevelCombo.setBounds(183, 75, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.configLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "configLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, SettingsDialog.this.configLevelCombo.getText());
									}
								});
							}
							{
								this.utilsLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.utilsLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.utilsLevelLabel.setLayout(cLabel4Layout);
								this.utilsLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0346));
								this.utilsLevelLabel.setBounds(3, 99, 170, 20);
							}
							{
								this.utilsLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.utilsLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.utilsLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.utilsLevelCombo.setBounds(183, 99, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.utilsLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "utilsLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.UTILS_LOG_LEVEL, SettingsDialog.this.utilsLevelCombo.getText());
									}
								});
							}
							{
								this.fileIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								this.fileIOLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.fileIOLevelLabel.setLayout(cLabel4Layout);
								this.fileIOLevelLabel.setBounds(3, 124, 170, 20);
								this.fileIOLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0347));
							}
							{
								this.fileIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.fileIOLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.fileIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.fileIOLevelCombo.setBounds(183, 124, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.fileIOLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "fileIOLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, SettingsDialog.this.fileIOLevelCombo.getText());
									}
								});
							}
							{
								this.serialIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
								this.serialIOLevelLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
								this.serialIOLevelLabel.setLayout(cLabel4Layout);
								this.serialIOLevelLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0348));
								this.serialIOLevelLabel.setBounds(3, 149, 170, 20);
							}
							{
								this.serialIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
								this.serialIOLevelCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
								this.serialIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
								this.serialIOLevelCombo.setBounds(183, 149, 79, OSDE.IS_WINDOWS ? 21 : 25);
								this.serialIOLevelCombo.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										SettingsDialog.log.log(Level.FINEST, "serialIOLevelCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										SettingsDialog.this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, SettingsDialog.this.serialIOLevelCombo.getText());
									}
								});
							}

						} // end individual package based logging settings
						{
							this.classBasedLabel = new Label(this.loggingGroup, SWT.CENTER);
							this.classBasedLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.classBasedLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0349));
							this.classBasedLabel.setBounds(286, 95, 192, 18);

							this.popupmenu = new Menu(this.dialogShell, SWT.POP_UP);
							this.logLevelMenu.createMenu(this.popupmenu);

							this.tree = new Tree(this.loggingGroup, SWT.NONE);
							this.tree.setLayout(null);
							this.tree.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
							this.tree.setBounds(286, 120, 192, 290);
							this.tree.setMenu(this.popupmenu);
							this.tree.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									SettingsDialog.log.log(Level.FINEST, "tree.widgetSelected, event=" + evt + " Selection:" + SettingsDialog.this.tree.getSelection()[0]); //$NON-NLS-1$ //$NON-NLS-2$
									TreeItem tmpItem = (TreeItem) evt.item;
									if (tmpItem.getParentItem() != null) {
										StringBuilder sb = new StringBuilder();
										TreeItem tmpParent = tmpItem;
										while (null != (tmpParent = tmpParent.getParentItem())) {
											sb.append(tmpParent.getText()).append("."); //$NON-NLS-1$
										}
										sb.append(tmpItem.getText());
										String loggerName = sb.toString();
										SettingsDialog.this.popupmenu.setData(SettingsDialog.LOGGER_NAME, loggerName);
									}
								}
							});
						}
					} // end logging group
					//} // end analysis composite
				} // end analysis tab item
				this.settingsTabFolder.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent evt) {
						SettingsDialog.log.log(Level.FINEST, "tree.focusGained, event=" + evt); //$NON-NLS-1$
						SettingsDialog.this.tree.clearAll(true);
						LogManager manager = LogManager.getLogManager();
						Enumeration<String> loggerNames = manager.getLoggerNames();
						StringBuilder sb = new StringBuilder();
						while (loggerNames.hasMoreElements()) {
							String loggerName = loggerNames.nextElement();
							if (loggerName.startsWith("osde") && loggerName.replace('.', ':').split(":").length >= 3) { //$NON-NLS-1$ //$NON-NLS-2$
								sb.append(loggerName).append(";"); //$NON-NLS-1$
							}
						}
						String[] loggers = sb.toString().split(";"); //$NON-NLS-1$
						Arrays.sort(loggers);
						if (SettingsDialog.log.isLoggable(Level.FINER)) {
							for (String string : loggers) {
								SettingsDialog.log.log(Level.FINER, string);
							}
						}
						SettingsDialog.this.tree.removeAll();
						String root = ""; //$NON-NLS-1$
						TreeItem treeItemRoot = null;
						TreeItem treeItemNode;
						for (String string : loggers) {
							String[] tmp = string.replace('.', ':').split(":"); //$NON-NLS-1$
							switch (tmp.length) {
							case 3:
								if (!root.equals(tmp[0] + "." + tmp[1])) { //$NON-NLS-1$
									root = tmp[0] + "." + tmp[1]; //$NON-NLS-1$
									treeItemRoot = new TreeItem(SettingsDialog.this.tree, SWT.SINGLE);
									treeItemRoot.setText(root);
								}
								treeItemNode = new TreeItem(treeItemRoot, SWT.NULL);
								treeItemNode.setText(tmp[2]);
								break;
							case 4:
								if (!root.equals(tmp[0] + "." + tmp[1] + "." + tmp[2])) { //$NON-NLS-1$ //$NON-NLS-2$
									root = tmp[0] + "." + tmp[1] + "." + tmp[2]; //$NON-NLS-1$ //$NON-NLS-2$
									treeItemRoot = new TreeItem(SettingsDialog.this.tree, SWT.SINGLE);
									treeItemRoot.setText(root);
								}
								treeItemNode = new TreeItem(treeItemRoot, SWT.NULL);
								treeItemNode.setText(tmp[3]);
								break;
							}
						}
					}
				});
				this.settingsTabFolder.setSelection(0);
			} // end tab folder

			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					SettingsDialog.log.log(Level.FINE, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					SettingsDialog.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_1.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					SettingsDialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (SettingsDialog.this.settings.getActiveDevice().startsWith(Settings.EMPTY)) SettingsDialog.this.settings.setActiveDevice(Settings.EMPTY_SIGNATURE);
					SettingsDialog.this.settings.store();
					if (SettingsDialog.this.settings.isGlobalSerialPort()) SettingsDialog.this.application.setGloabalSerialPort(SettingsDialog.this.serialPort.getText());
					// set logging levels
					SettingsDialog.this.settings.updateLogLevel();
					// check if black or white list enabled and some chars typed but not accepted
					if (SettingsDialog.this.settings.isSerialPortBlackListEnabled()) {
						SettingsDialog.this.settings.setSerialPortBlackList(SettingsDialog.this.serialPortBlackList.getText());
					}
					else if (SettingsDialog.this.settings.isSerialPortWhiteListEnabled()) {
						SettingsDialog.this.settings.setSerialPortWhiteList(SettingsDialog.this.serialPortWhiteList.getText());
					}
					// check for changed local
					if (SettingsDialog.this.isLocaleLanguageChanged) {
						SettingsDialog.this.application.openMessageDialog(SettingsDialog.this.dialogShell, Messages.getString(MessageIds.OSDE_MSGT0304));
					}
				}
			});
			{ // begin ok button
				FormData okButtonLData = new FormData();
				okButtonLData.width = 250;
				okButtonLData.height = 30;
				okButtonLData.left = new FormAttachment(0, 1000, 116);
				okButtonLData.bottom = new FormAttachment(1000, 1000, -9);
				okButtonLData.right = new FormAttachment(1000, 1000, -122);
				this.okButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okButton.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
				this.okButton.setLayoutData(okButtonLData);
				this.okButton.setText(Messages.getString(MessageIds.OSDE_MSGT0188));
				this.okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						SettingsDialog.log.log(Level.FINEST, "okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						SettingsDialog.this.dialogShell.dispose();
					}
				});
			} // end ok button

			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
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
			if (this.supportedLocals[index].equals(language)) return index;
		}
		return index;
	}

	/**
	 * query the available serial ports and update the serialPortGroup combo
	 */
	void updateAvailablePorts() {
		// execute independent from dialog UI
		this.listPortsThread = new Thread() {
			@Override
			public void run() {
				try {
					while (!SettingsDialog.this.dialogShell.isDisposed()) {
						SettingsDialog.this.availablePorts = DeviceSerialPort.listConfiguredSerialPorts(SettingsDialog.this.settings.doPortAvailabilityCheck(), 
								SettingsDialog.this.settings.isSerialPortBlackListEnabled() ? SettingsDialog.this.settings.getSerialPortBlackList() : OSDE.STRING_EMPTY,
								SettingsDialog.this.settings.isSerialPortWhiteListEnabled() ? SettingsDialog.this.settings.getSerialPortWhiteList() : new Vector<String>());
						OpenSerialDataExplorer.display.syncExec(new Runnable() {
							public void run() {
								if (SettingsDialog.this.dialogShell != null && !SettingsDialog.this.dialogShell.isDisposed()) {
									if (SettingsDialog.this.availablePorts != null && SettingsDialog.this.availablePorts.size() > 0) {
										SettingsDialog.this.serialPort.setItems(StringHelper.prepareSerialPortList(SettingsDialog.this.availablePorts));
										int index = SettingsDialog.this.availablePorts.indexOf(SettingsDialog.this.settings.getSerialPort());
										if (index > -1) {
											SettingsDialog.this.serialPort.select(index);
										}
										else {
											SettingsDialog.this.serialPort.setText(Messages.getString(MessageIds.OSDE_MSGT0197));
										}
									}
									else {
										SettingsDialog.this.serialPort.setItems(new String[0]);
										SettingsDialog.this.serialPort.setText(Messages.getString(MessageIds.OSDE_MSGT0198));
									}
								}
							}
							});
						try {
							Thread.sleep(2500);
						}
						catch (InterruptedException e) {
							// ignore
						}
					}
				}
				catch (Throwable t) {
					SettingsDialog.log.log(Level.WARNING, t.getMessage(), t);
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
		this.tree.setEnabled(value);
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
