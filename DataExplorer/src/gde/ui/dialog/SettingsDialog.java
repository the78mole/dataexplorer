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
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class to adjust application wide properties
 * @author Winfried Brügmann
 */
public class SettingsDialog extends Dialog {
	final static Logger						log							= Logger.getLogger(SettingsDialog.class.getName());

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
	Button												doPortAvailabilityCheck;
	Button												suggestObjectKey;
	Button												suggestDate;
	Group													fileOpenSaveDialogGroup;
	CLabel												fileIOLevelLabel;
	CCombo												fileIOLevelCombo;
	Button												deviceDialogButton;
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
	Vector<String>								availablePorts	= new Vector<String>();
	final Settings								settings;
	final OpenSerialDataExplorer	application;

	public SettingsDialog(Shell parent, int style) {
		super(parent, style);
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	public void open() {
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(496, 641);
			this.dialogShell.setText("OpenSerialDataExplorer - Settings");
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.gif"));
			{
				FormData fileOpenSaveDialogGroupLData = new FormData();
				fileOpenSaveDialogGroupLData.width = 451;
				fileOpenSaveDialogGroupLData.height = 44;
				fileOpenSaveDialogGroupLData.left = new FormAttachment(0, 1000, 12);
				fileOpenSaveDialogGroupLData.top = new FormAttachment(0, 1000, 77);
				this.fileOpenSaveDialogGroup = new Group(this.dialogShell, SWT.NONE);
				this.fileOpenSaveDialogGroup.setLayout(null);
				this.fileOpenSaveDialogGroup.setLayoutData(fileOpenSaveDialogGroupLData);
				this.fileOpenSaveDialogGroup.setText("Dateinamen Sicherndialog");
				this.fileOpenSaveDialogGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SettingsDialog.log.fine("fileOpenSaveDialogGroup.paintControl, event=" + evt);
						SettingsDialog.this.suggestDate.setSelection(SettingsDialog.this.settings.getUsageDateAsFileNameLeader());
						SettingsDialog.this.suggestObjectKey.setSelection(SettingsDialog.this.settings.getUsageObjectKeyInFileName());
					}
				});
				{
					this.suggestDate = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
					this.suggestDate.setText("Datum als Dateianfang");
					this.suggestDate.setBounds(27, 28, 194, 16);
					this.suggestDate.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.fine("suggestDate.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.setUsageDateAsFileNameLeader(SettingsDialog.this.suggestDate.getSelection());
						}
					});
				}
				{
					this.suggestObjectKey = new Button(this.fileOpenSaveDialogGroup, SWT.CHECK | SWT.LEFT);
					this.suggestObjectKey.setText("Objektschlüssel verwenden");
					this.suggestObjectKey.setBounds(239, 28, 194, 16);
					this.suggestObjectKey.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.fine("suggestObjectKey.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.setUsageObjectKeyInFileName(SettingsDialog.this.suggestObjectKey.getSelection());
						}
					});
				}
			}
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					SettingsDialog.log.fine("dialogShell.helpRequested, event=" + evt);
					SettingsDialog.this.application.openHelpDialog("", "HelpInfo_1.html");
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					SettingsDialog.log.finest("dialogShell.widgetDisposed, event=" + evt);
					if (SettingsDialog.this.settings.getActiveDevice().startsWith("---")) SettingsDialog.this.settings.setActiveDevice("---;---;---");
					SettingsDialog.this.settings.store();
					if (SettingsDialog.this.settings.isGlobalSerialPort()) SettingsDialog.this.application.setGloabalSerialPort(SettingsDialog.this.serialPort.getText());
					// set logging levels
					SettingsDialog.this.settings.updateLogLevel();
				}
			});
			{
				FormData GerätedialogLData = new FormData();
				GerätedialogLData.width = 451;
				GerätedialogLData.height = 35;
				GerätedialogLData.left = new FormAttachment(0, 1000, 12);
				GerätedialogLData.top = new FormAttachment(0, 1000, 215);
				this.deviceDialogGroup = new Group(this.dialogShell, SWT.NONE);
				this.deviceDialogGroup.setLayout(null);
				this.deviceDialogGroup.setLayoutData(GerätedialogLData);
				this.deviceDialogGroup.setText("Gerätedialoge");
				this.deviceDialogGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SettingsDialog.log.finest("deviceDialogGroup.paintControl, event=" + evt);
						SettingsDialog.this.deviceDialogButton.setSelection(SettingsDialog.this.settings.isDeviceDialogsModal());
					}
				});
				{
					this.deviceDialogButton = new Button(this.deviceDialogGroup, SWT.CHECK | SWT.LEFT);
					this.deviceDialogButton.setText("    Gerätedialoge anwendungsmodal einstellen");
					this.deviceDialogButton.setBounds(65, 24, 327, 16);
					this.deviceDialogButton.setToolTipText("Hiermit stellt man ein ob die Gerätedialoge erst geschlosen werden müssen bevor man an die Hauptfenster herankommt");
					this.deviceDialogButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.finest("deviceDialogButton.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.enabelModalDeviceDialogs(SettingsDialog.this.deviceDialogButton.getSelection());
						}
					});
				}
			}
			{ // begin default data path group
				this.defaultDataPathGroup = new Group(this.dialogShell, SWT.NONE);
				this.defaultDataPathGroup.setLayout(null);
				FormData group1LData = new FormData();
				group1LData.width = 534;
				group1LData.height = 42;
				group1LData.left = new FormAttachment(0, 1000, 12);
				group1LData.top = new FormAttachment(0, 1000, 12);
				group1LData.right = new FormAttachment(1000, 1000, -19);
				this.defaultDataPathGroup.setLayoutData(group1LData);
				this.defaultDataPathGroup.setText("Standarddatenpfad");
				this.defaultDataPathGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SettingsDialog.log.finest("defaultDataPathGroup.paintControl, event=" + evt);
						SettingsDialog.this.defaultDataPath.setText(SettingsDialog.this.settings.getDataFilePath());
					}
				});
				{
					this.defaultDataPathLabel = new CLabel(this.defaultDataPathGroup, SWT.NONE);
					this.defaultDataPathLabel.setText("Datenpfad  :    ");
					this.defaultDataPathLabel.setBounds(14, 24, 90, 20);
				}
				{
					this.defaultDataPath = new Text(this.defaultDataPathGroup, SWT.BORDER);
					this.defaultDataPath.setBounds(107, 24, 295, 20);
				}
				{
					this.defaultDataPathAdjustButton = new Button(this.defaultDataPathGroup, SWT.PUSH | SWT.CENTER);
					this.defaultDataPathAdjustButton.setText(". . . ");
					this.defaultDataPathAdjustButton.setBounds(405, 24, 30, 20);
					this.defaultDataPathAdjustButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.finest("defaultDataPathAdjustButton.widgetSelected, event=" + evt);
							String defaultDataDirectory = SettingsDialog.this.application.openDirFileDialog("Einstellungen - Standard Daten Pfad", SettingsDialog.this.settings.getDataFilePath());
							SettingsDialog.log.fine("default directory from directoy dialog = " + defaultDataDirectory);
							SettingsDialog.this.settings.setDataFilePath(defaultDataDirectory);
							SettingsDialog.this.defaultDataPath.setText(defaultDataDirectory);
						}
					});
				}
			} // end default data path group
			{ // begin separator group
				this.separatorGroup = new Group(this.dialogShell, SWT.NONE);
				this.separatorGroup.setLayout(null);
				FormData separatorGroupLData = new FormData();
				separatorGroupLData.width = 451;
				separatorGroupLData.height = 44;
				separatorGroupLData.left = new FormAttachment(0, 1000, 12);
				separatorGroupLData.top = new FormAttachment(0, 1000, 145);
				separatorGroupLData.right = new FormAttachment(1000, 1000, -19);
				this.separatorGroup.setLayoutData(separatorGroupLData);
				this.separatorGroup.setText("Zahlen- CSV-Separator");
				this.separatorGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SettingsDialog.log.finest("separatorGroup.paintControl, event=" + evt);
						SettingsDialog.this.decimalSeparator.setText(SettingsDialog.this.settings.getDecimalSeparator() + "");
						SettingsDialog.this.listSeparator.setText(SettingsDialog.this.settings.getListSeparator() + "");
					}
				});
				{
					this.decimalSeparatorLabel = new CLabel(this.separatorGroup, SWT.NONE);
					this.decimalSeparatorLabel.setText("Dezimalseparator : ");
					this.decimalSeparatorLabel.setToolTipText("Der Dezimalseparator is abhängig von den eingestellten Systemlocalen, einige Betribssysteme erlauben aber davon abweichende Konfiguration");
					this.decimalSeparatorLabel.setBounds(28, 24, 122, 22);
				}
				{
					this.decimalSeparator = new CCombo(this.separatorGroup, SWT.BORDER);
					this.decimalSeparator.setItems(new String[] { " . ", " , " });
					this.decimalSeparator.setBounds(153, 24, 43, 20);
					this.decimalSeparator.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.finest("decimalSeparator.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.setDecimalSeparator(SettingsDialog.this.decimalSeparator.getText().trim());
							SettingsDialog.this.decimalSeparator.setText(" " + SettingsDialog.this.decimalSeparator.getText().trim() + " ");
						}
					});
				}
				{
					this.listSeparatorLabel = new CLabel(this.separatorGroup, SWT.NONE);
					this.listSeparatorLabel.setText("Listenseparator : ");
					this.listSeparatorLabel.setToolTipText("Der Listenseparator is abhängig von den eingestellten Systemlocalen, einige Betribssysteme erlauben aber davon abweichende Konfiguration");
					this.listSeparatorLabel.setBounds(258, 24, 108, 22);
				}
				{
					this.listSeparator = new CCombo(this.separatorGroup, SWT.BORDER);
					this.listSeparator.setItems(new String[] { " , ", " ; " });
					this.listSeparator.setBounds(369, 24, 47, 20);
					this.listSeparator.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.finest("listSeparator.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.setListSeparator(SettingsDialog.this.listSeparator.getText().trim());
							SettingsDialog.this.listSeparator.setText(" " + SettingsDialog.this.listSeparator.getText().trim() + " ");
						}
					});
				}
			} // end separator group
			{ // begin serial port group
				this.serialPortGroup = new Group(this.dialogShell, SWT.NONE);
				this.serialPortGroup.setLayout(null);
				FormData serialPortGroupLData = new FormData();
				serialPortGroupLData.width = 451;
				serialPortGroupLData.height = 55;
				serialPortGroupLData.left = new FormAttachment(0, 1000, 12);
				serialPortGroupLData.right = new FormAttachment(1000, 1000, -19);
				serialPortGroupLData.top = new FormAttachment(0, 1000, 275);
				this.serialPortGroup.setLayoutData(serialPortGroupLData);
				this.serialPortGroup.setText("Serial Port Adjustment");
				this.serialPortGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SettingsDialog.log.finest("serialPortGroup.paintControl, event=" + evt);
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
					this.doPortAvailabilityCheck.setBounds(15, 19, 340, 22);
					this.doPortAvailabilityCheck.setText("Verfügbarkeitsprüfung während Scan");
					this.doPortAvailabilityCheck.setToolTipText("Hier wird der serielle Port kurz geöffnet und geschlossen,  das kostet Systeminterrupts und damit Zeit. ");
					this.doPortAvailabilityCheck.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.fine("doPortAvailabilityCheck.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.setPortAvailabilityCheck(SettingsDialog.this.doPortAvailabilityCheck.getSelection());
						}
					});
				}
				{
					this.useGlobalSerialPort = new Button(this.serialPortGroup, SWT.CHECK | SWT.LEFT);
					this.useGlobalSerialPort.setText(" globale serielle Port Konfiguration ");
					this.useGlobalSerialPort.setToolTipText("Steht dieser Schalter angewählt wird Anwendungsweit nur ein serieller Port verwendet, sonst wird pro Gerät eine eigene Portkonfiguration verwendet");
					this.useGlobalSerialPort.setBounds(15, 43, 225, 20);
					this.useGlobalSerialPort.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.finest("useGlobalSerialPort.widgetSelected, event=" + evt);
							if (SettingsDialog.this.useGlobalSerialPort.getSelection()) {
								SettingsDialog.this.settings.setIsGlobalSerialPort("true");
								updateAvailablePorts();
							}
							else {
								SettingsDialog.this.settings.setIsGlobalSerialPort("false");
							}
						}
					});
				}
				{
					this.serialPort = new CCombo(this.serialPortGroup, SWT.BORDER);
					this.serialPort.setBounds(240, 43, 130, 20);
					this.serialPort.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SettingsDialog.log.finest("serialPort.widgetSelected, event=" + evt);
							SettingsDialog.this.settings.setSerialPort(SettingsDialog.this.serialPort.getText());
						}
					});
				}
				{
					this.Port = new CLabel(this.serialPortGroup, SWT.NONE);
					this.Port.setText("serieller Port");
					this.Port.setBounds(370, 43, 81, 22);
				}
			} // end serial port group
			{ // begin logging group
				this.loggingGroup = new Group(this.dialogShell, SWT.NONE);
				this.loggingGroup.setLayout(null);
				FormData loggingGroupLData = new FormData();
				loggingGroupLData.width = 451;
				loggingGroupLData.height = 184;
				loggingGroupLData.left = new FormAttachment(0, 1000, 12);
				loggingGroupLData.top = new FormAttachment(0, 1000, 355);
				loggingGroupLData.right = new FormAttachment(1000, 1000, -19);
				this.loggingGroup.setLayoutData(loggingGroupLData);
				this.loggingGroup.setText("Debug Logging Level");
				this.loggingGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SettingsDialog.log.finest("loggingGroup.paintControl, event=" + evt);
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
				{

					this.globalLoggingComposite = new Composite(this.loggingGroup, SWT.NONE);
					this.globalLoggingComposite.setLayout(null);
					this.globalLoggingComposite.setBounds(6, 19, 154, 50);
					{
						this.globalLogLevel = new Button(this.globalLoggingComposite, SWT.CHECK | SWT.LEFT);

						this.globalLogLevel.setText(" globaler Log Level");
						this.globalLogLevel.setBounds(4, 3, 148, 21);
						this.globalLogLevel.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("globalLogLevel.widgetSelected, event=" + evt);
								if (SettingsDialog.this.globalLogLevel.getSelection()) {
									enableIndividualLogging(false);
									SettingsDialog.this.globalLoggingCombo.setEnabled(true);
									SettingsDialog.this.settings.setIsGlobalLogLevel("true");
								}
								else {
									enableIndividualLogging(true);
									SettingsDialog.this.globalLoggingCombo.setEnabled(false);
									SettingsDialog.this.settings.setIsGlobalLogLevel("false");
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
								SettingsDialog.log.finest("globalLoggingCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, SettingsDialog.this.globalLoggingCombo.getText());
								SettingsDialog.this.globalLoggingCombo.setText(SettingsDialog.this.globalLoggingCombo.getText());
							}
						});
					}
				}
				{
					this.individualLoggingComosite = new Composite(this.loggingGroup, SWT.NONE);
					this.individualLoggingComosite.setLayout(null);
					this.individualLoggingComosite.setBounds(172, 19, 278, 178);
					{
						this.uiLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout uiLevelLabelLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						this.uiLevelLabel.setLayout(uiLevelLabelLayout);
						this.uiLevelLabel.setText("Graphische Oberfläche : ");
						this.uiLevelLabel.setBounds(3, 3, 170, 20);
					}
					{
						this.uiLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.uiLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.uiLevelCombo.setBounds(183, 3, 79, 21);
						this.uiLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("uiLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.UI_LOG_LEVEL, SettingsDialog.this.uiLevelCombo.getText());
							}
						});
					}
					{
						this.deviceLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout cLabel1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						this.deviceLevelLabel.setLayout(cLabel1Layout);
						this.deviceLevelLabel.setText("Geräte :");
						this.deviceLevelLabel.setBounds(3, 27, 170, 20);
					}
					{
						this.deviceLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.deviceLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.deviceLevelCombo.setBounds(183, 27, 79, 21);
						this.deviceLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("deviceLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, SettingsDialog.this.deviceLevelCombo.getText());
							}
						});
					}
					{
						this.commonLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout cLabel2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						this.commonLevelLabel.setLayout(cLabel2Layout);
						this.commonLevelLabel.setText("Datenmodell : ");
						this.commonLevelLabel.setBounds(3, 51, 170, 20);
					}
					{
						this.commonLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.commonLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.commonLevelCombo.setBounds(183, 51, 79, 21);
						this.commonLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("commonLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.DATA_LOG_LEVEL, SettingsDialog.this.commonLevelCombo.getText());
							}
						});
					}
					{
						this.configLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout cLabel3Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						this.configLevelLabel.setLayout(cLabel3Layout);
						this.configLevelLabel.setText("Konfiguration :");
						this.configLevelLabel.setBounds(3, 75, 170, 20);
					}
					{
						this.configLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.configLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.configLevelCombo.setBounds(183, 75, 79, 21);
						this.configLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("configLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, SettingsDialog.this.configLevelCombo.getText());
							}
						});
					}
					{
						this.utilsLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						this.utilsLevelLabel.setLayout(cLabel4Layout);
						this.utilsLevelLabel.setText("allgem. Funktionen : ");
						this.utilsLevelLabel.setBounds(3, 99, 170, 20);
					}
					{
						this.utilsLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.utilsLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.utilsLevelCombo.setBounds(183, 99, 79, 21);
						this.utilsLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("utilsLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.UTILS_LOG_LEVEL, SettingsDialog.this.utilsLevelCombo.getText());
							}
						});
					}
					{
						this.fileIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.fileIOLevelLabel.setLayout(cLabel4Layout);
						this.fileIOLevelLabel.setBounds(3, 124, 170, 20);
						this.fileIOLevelLabel.setText("Datei I/O : ");
					}
					{
						this.fileIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.fileIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.fileIOLevelCombo.setBounds(183, 124, 79, 21);
						this.fileIOLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("fileIOLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, SettingsDialog.this.fileIOLevelCombo.getText());
							}
						});
					}
					{
						this.serialIOLevelLabel = new CLabel(this.individualLoggingComosite, SWT.NONE);
						RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						this.serialIOLevelLabel.setLayout(cLabel4Layout);
						this.serialIOLevelLabel.setText("serial I/O : ");
						this.serialIOLevelLabel.setBounds(3, 149, 170, 20);
					}
					{
						this.serialIOLevelCombo = new CCombo(this.individualLoggingComosite, SWT.BORDER);
						this.serialIOLevelCombo.setItems(Settings.LOGGING_LEVEL);
						this.serialIOLevelCombo.setBounds(183, 149, 79, 21);
						this.serialIOLevelCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SettingsDialog.log.finest("serialIOLevelCombo.widgetSelected, event=" + evt);
								SettingsDialog.this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, SettingsDialog.this.serialIOLevelCombo.getText());
							}
						});
					}

				}
			} // end logging group
			{ // begin ok button
				FormData okButtonLData = new FormData();
				okButtonLData.width = 250;
				okButtonLData.height = 25;
				okButtonLData.left = new FormAttachment(0, 1000, 116);
				okButtonLData.right = new FormAttachment(1000, 1000, -122);
				okButtonLData.bottom = new FormAttachment(1000, 1000, -12);
				this.okButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okButton.setLayoutData(okButtonLData);
				this.okButton.setText("OK");
				this.okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						SettingsDialog.log.finest("okButton.widgetSelected, event=" + evt);
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
			this.settings.setProperty(Settings.GLOBAL_LOG_LEVEL, "INFO");
		}
		this.globalLoggingCombo.setText(this.settings.getProperty(Settings.GLOBAL_LOG_LEVEL));

		if (this.settings.getProperty(Settings.UI_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.UI_LOG_LEVEL, "INFO");
		}
		this.uiLevelCombo.setText(this.settings.getProperty(Settings.UI_LOG_LEVEL));

		if (this.settings.getProperty(Settings.DATA_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.DATA_LOG_LEVEL, "INFO");
		}
		this.commonLevelCombo.setText(this.settings.getProperty(Settings.DATA_LOG_LEVEL));

		if (this.settings.getProperty(Settings.DEVICE_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.DEVICE_LOG_LEVEL, "INFO");
		}
		this.deviceLevelCombo.setText(this.settings.getProperty(Settings.DEVICE_LOG_LEVEL));

		if (this.settings.getProperty(Settings.CONFIG_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.CONFIG_LOG_LEVEL, "INFO");
		}
		this.configLevelCombo.setText(this.settings.getProperty(Settings.CONFIG_LOG_LEVEL));

		if (this.settings.getProperty(Settings.UTILS_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.UTILS_LOG_LEVEL, "INFO");
		}
		this.utilsLevelCombo.setText(this.settings.getProperty(Settings.UTILS_LOG_LEVEL));

		if (this.settings.getProperty(Settings.FILE_IO_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.FILE_IO_LOG_LEVEL, "INFO");
		}
		this.fileIOLevelCombo.setText(this.settings.getProperty(Settings.FILE_IO_LOG_LEVEL));

		if (this.settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL) == null) {
			this.settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, "INFO");
		}
		this.serialIOLevelCombo.setText(this.settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL));
	}
}
