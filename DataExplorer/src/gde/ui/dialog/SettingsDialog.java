/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.dialog;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.device.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * @author Winfried Brügmann
 * dialog class to adjust application wide properties
 */
public class SettingsDialog extends org.eclipse.swt.widgets.Dialog {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private CCombo												configLevelCombo;
	private CLabel												utilsLevelLabel;
	private CCombo												utilsLevelCombo;
	private CLabel												serialIOLevelLabel;
	private CCombo												serialIOLevelCombo;
	private CLabel												configLevelLabel;
	private Button												okButton;
	private Button												globalLogLevel;
	private CLabel												commonLevelLabel;
	private CCombo												commonLevelCombo;
	private CLabel												deviceLevelLabel;
	private CCombo												deviceLevelCombo;
	private CCombo												uiLevelCombo;
	private CLabel												uiLevelLabel;
	private Composite											individualLoggingComosite;
	private Composite											globalLoggingComposite;
	private Shell													dialogShell;
	private CLabel												defaultDataPathLabel;
	private Group													defaultDataPathGroup;
	private CLabel												Port;
	private CCombo												serialPort;
	private Button												useGlobalSerialPort;
	private Group													serialPortGroup;
	private Group													separatorGroup;
	private CCombo												listSeparator;
	private CLabel												listSeparatorLabel;
	private CLabel												space;
	private CCombo												decimalSeparator;
	private CLabel												decimalSeparatorLabel;
	private Button												defaultDataPathAdjustButton;
	private Text													defaultDataPath;
	private CCombo												globalLoggingCombo;
	private Group													loggingGroup;

	private final Settings								settings;
	private final OpenSerialDataExplorer	application;

	public SettingsDialog(Shell parent, int style) {
		super(parent, style);
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	public void open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(496, 514);
			dialogShell.setText("OpenSerialDataExplorer - Settings");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.gif"));
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.finest("dialogShell.widgetDisposed, event=" + evt);
					if (settings.getActiveDevice().startsWith("---")) settings.setActiveDevice("---;---;---");
					settings.store();
					if (settings.isGlobalSerialPort()) application.setGloabalSerialPort(serialPort.getText());
					// set logging levels
					settings.updateLogLevel();
				}
			});
			{ // begin default data path group
				defaultDataPathGroup = new Group(dialogShell, SWT.NONE);
				RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				defaultDataPathGroup.setLayout(group1Layout);
				FormData group1LData = new FormData();
				group1LData.width = 534;
				group1LData.height = 42;
				group1LData.left = new FormAttachment(0, 1000, 12);
				group1LData.top = new FormAttachment(0, 1000, 12);
				group1LData.right = new FormAttachment(1000, 1000, -19);
				defaultDataPathGroup.setLayoutData(group1LData);
				defaultDataPathGroup.setText("Standarddatenpfad");
				defaultDataPathGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("defaultDataPathGroup.paintControl, event=" + evt);
						defaultDataPath.setText(settings.getDataFilePath());
					}
				});
				{
					defaultDataPathLabel = new CLabel(defaultDataPathGroup, SWT.NONE);
					RowData defaultDataPathLData = new RowData();
					defaultDataPathLData.width = 90;
					defaultDataPathLData.height = 20;
					defaultDataPathLabel.setLayoutData(defaultDataPathLData);
					defaultDataPathLabel.setText("Datenpfad  :    ");
				}
				{
					RowData defaultDataPathTextLData = new RowData();
					defaultDataPathTextLData.width = 295;
					defaultDataPathTextLData.height = 20;
					defaultDataPath = new Text(defaultDataPathGroup, SWT.NONE);
					defaultDataPath.setLayoutData(defaultDataPathTextLData);
				}
				{
					defaultDataPathAdjustButton = new Button(defaultDataPathGroup, SWT.PUSH | SWT.CENTER);
					RowData defaultDataPathAdjustButtonLData = new RowData();
					defaultDataPathAdjustButtonLData.width = 30;
					defaultDataPathAdjustButtonLData.height = 20;
					defaultDataPathAdjustButton.setLayoutData(defaultDataPathAdjustButtonLData);
					defaultDataPathAdjustButton.setText(". . . ");
					defaultDataPathAdjustButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("defaultDataPathAdjustButton.widgetSelected, event=" + evt);
							String defaultDataDirectory = application.openDirFileDialog("Einstellungen - Standard Daten Pfad", settings.getDataFilePath());
							log.fine("default directory from directoy dialog = " + defaultDataDirectory);
							settings.setDataFilePath(defaultDataDirectory);
							defaultDataPath.setText(defaultDataDirectory);
						}
					});
				}
			} // end default data path group
			{ // begin separator group
				separatorGroup = new Group(dialogShell, SWT.NONE);
				RowLayout separatorGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				separatorGroup.setLayout(separatorGroupLayout);
				FormData separatorGroupLData = new FormData();
				separatorGroupLData.width = 534;
				separatorGroupLData.height = 44;
				separatorGroupLData.left = new FormAttachment(0, 1000, 12);
				separatorGroupLData.top = new FormAttachment(0, 1000, 85);
				separatorGroupLData.right = new FormAttachment(1000, 1000, -19);
				separatorGroup.setLayoutData(separatorGroupLData);
				separatorGroup.setText("Zahlen- CSV-Separator");
				separatorGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("separatorGroup.paintControl, event=" + evt);
						decimalSeparator.setText(settings.getDecimalSeparator() + "");
						listSeparator.setText(settings.getListSeparator() + "");
					}
				});
				{
					decimalSeparatorLabel = new CLabel(separatorGroup, SWT.NONE);
					decimalSeparatorLabel.setText("Dezimalseparator : ");
					decimalSeparatorLabel.setToolTipText("Der Dezimalseparator is abhängig von den eingestellten Systemlocalen, einige Betribssysteme erlauben aber davon abweichende Konfiguration");
				}
				{
					RowData decimalSeparatorLData = new RowData();
					decimalSeparatorLData.width = 43;
					decimalSeparatorLData.height = 20;
					decimalSeparator = new CCombo(separatorGroup, SWT.NONE);
					decimalSeparator.setLayoutData(decimalSeparatorLData);
					decimalSeparator.setItems(new String[] { " . ", " , " });
					decimalSeparator.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("decimalSeparator.widgetSelected, event=" + evt);
							settings.setDecimalSeparator(decimalSeparator.getText().trim());
							decimalSeparator.setText(" " + decimalSeparator.getText().trim() + " ");
						}
					});
				}
				{
					RowData spaceLData = new RowData();
					spaceLData.width = 89;
					spaceLData.height = 20;
					space = new CLabel(separatorGroup, SWT.NONE);
					space.setLayoutData(spaceLData);
				}
				{
					listSeparatorLabel = new CLabel(separatorGroup, SWT.NONE);
					listSeparatorLabel.setText("Listenseparator : ");
					listSeparatorLabel.setToolTipText("Der Listenseparator is abhängig von den eingestellten Systemlocalen, einige Betribssysteme erlauben aber davon abweichende Konfiguration");
				}
				{
					RowData listSeparatorLData = new RowData();
					listSeparatorLData.width = 47;
					listSeparatorLData.height = 20;
					listSeparator = new CCombo(separatorGroup, SWT.NONE);
					listSeparator.setLayoutData(listSeparatorLData);
					listSeparator.setItems(new String[] { " , ", " ; " });
					listSeparator.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("listSeparator.widgetSelected, event=" + evt);
							settings.setListSeparator(listSeparator.getText().trim());
							listSeparator.setText(" " + listSeparator.getText().trim() + " ");
						}
					});
				}
			} // end separator group
			{ // begin serial port group
				serialPortGroup = new Group(dialogShell, SWT.NONE);
				RowLayout serialPortGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				serialPortGroup.setLayout(serialPortGroupLayout);
				FormData serialPortGroupLData = new FormData();
				serialPortGroupLData.width = 454;
				serialPortGroupLData.height = 34;
				serialPortGroupLData.left = new FormAttachment(0, 1000, 12);
				serialPortGroupLData.right = new FormAttachment(1000, 1000, -19);
				serialPortGroupLData.top = new FormAttachment(0, 1000, 154);
				serialPortGroup.setLayoutData(serialPortGroupLData);
				serialPortGroup.setText("Serial Port Adjustment");
				serialPortGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("serialPortGroup.paintControl, event=" + evt);
						useGlobalSerialPort.setSelection(settings.isGlobalSerialPort());
						serialPort.setText(settings.getSerialPort());
						OpenSerialDataExplorer.display.asyncExec(new Runnable() {
							public void run() {
								serialPort.setItems(DeviceSerialPort.listConfiguredSerialPorts().toArray(new String[1]));
							}
						});
					}
				});
				{
					useGlobalSerialPort = new Button(serialPortGroup, SWT.CHECK | SWT.LEFT);
					useGlobalSerialPort.setText(" globale serielle Port Konfiguration ");
					RowData useGlobalSerialPortLData = new RowData();
					useGlobalSerialPortLData.width = 251;
					useGlobalSerialPortLData.height = 20;
					useGlobalSerialPort.setLayoutData(useGlobalSerialPortLData);
					useGlobalSerialPort.setToolTipText("Steht dieser Schalter auf ja, wird Anwendungsweit nur ein serieller Port verwendet, sonst kommt die Portkonfiguration aus den  Geräte INI Dateien.");
					useGlobalSerialPort.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("useGlobalSerialPort.widgetSelected, event=" + evt);
							if (useGlobalSerialPort.getSelection()) {
								settings.setIsGlobalSerialPort("true");								serialPort.setItems(DeviceSerialPort.listConfiguredSerialPorts().toArray(new String[1]));
							}
							else {
								settings.setIsGlobalSerialPort("false");
							}
						}
					});
				}
				{
					RowData serialPortLData = new RowData();
					serialPortLData.width = 84;
					serialPortLData.height = 20;
					serialPort = new CCombo(serialPortGroup, SWT.NONE);
					serialPort.setLayoutData(serialPortLData);
					serialPort.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("serialPort.widgetSelected, event=" + evt);
							settings.setSerialPort(serialPort.getText());
						}
					});
				}
				{
					Port = new CLabel(serialPortGroup, SWT.NONE);
					Port.setText("serieller Port");
				}
			} // end serial port group
			{ // begin logging group
				loggingGroup = new Group(dialogShell, SWT.NONE);
				RowLayout loggingGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				loggingGroup.setLayout(loggingGroupLayout);
				FormData loggingGroupLData = new FormData();
				loggingGroupLData.width = 460;
				loggingGroupLData.height = 195;
				loggingGroupLData.left =  new FormAttachment(0, 1000, 12);
				loggingGroupLData.top =  new FormAttachment(0, 1000, 219);
				loggingGroupLData.right =  new FormAttachment(1000, 1000, -19);
				loggingGroup.setLayoutData(loggingGroupLData);
				loggingGroup.setText("Debug Logging Level");
				loggingGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("loggingGroup.paintControl, event=" + evt);
						globalLogLevel.setSelection(settings.isGlobalLogLevel());
						if (settings.isGlobalLogLevel()) {
							enableIndividualLogging(false);
							globalLoggingCombo.setEnabled(true);
						}
						else {
							enableIndividualLogging(true);
							globalLoggingCombo.setEnabled(false);
							globalLogLevel.setSelection(false);
						}
						updateLoggingLevels();
					}
				});
				{
					RowData composite1LData = new RowData();
					composite1LData.width = 195;
					composite1LData.height = 50;
					globalLoggingComposite = new Composite(loggingGroup, SWT.NONE);
					RowLayout composite1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					globalLoggingComposite.setLayout(composite1Layout);
					globalLoggingComposite.setLayoutData(composite1LData);
					{
						globalLogLevel = new Button(globalLoggingComposite, SWT.CHECK | SWT.LEFT);
						RowData globalLogLevelLData = new RowData();
						globalLogLevelLData.width = 150;
						globalLogLevelLData.height = 20;
						globalLogLevel.setLayoutData(globalLogLevelLData);
						globalLogLevel.setText(" globaler Log Level");
						globalLogLevel.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("globalLogLevel.widgetSelected, event=" + evt);
								if (globalLogLevel.getSelection()) {
									enableIndividualLogging(false);
									globalLoggingCombo.setEnabled(true);
									settings.setIsGlobalLogLevel("true");
								}
								else {
									enableIndividualLogging(true);
									globalLoggingCombo.setEnabled(false);
									settings.setIsGlobalLogLevel("false");
								}

							}
						});
					}
					{
						space = new CLabel(globalLoggingComposite, SWT.NONE);
						RowData cLabel1LData1 = new RowData();
						cLabel1LData1.width = 41;
						cLabel1LData1.height = 20;
						space.setLayoutData(cLabel1LData1);
					}
					{
						globalLoggingCombo = new CCombo(globalLoggingComposite, SWT.NONE);
						globalLoggingCombo.setItems(new String[] { "SEVERE", "WARNING", "INFO", "FINE", "FINER", "FINEST" });
						globalLoggingCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("globalLoggingCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.GLOBAL_LOG_LEVEL, globalLoggingCombo.getText());
								globalLoggingCombo.setText(globalLoggingCombo.getText());
							}
						});
					}
				}
				{
					individualLoggingComosite = new Composite(loggingGroup, SWT.NONE);
					RowLayout individualLoggingComositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					RowData individualLoggingComositeLData = new RowData();
					individualLoggingComositeLData.width = 246;
					individualLoggingComositeLData.height = 184;
					individualLoggingComosite.setLayoutData(individualLoggingComositeLData);
					individualLoggingComosite.setLayout(individualLoggingComositeLayout);
					{
						uiLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout uiLevelLabelLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData cLabel1LData = new RowData();
						cLabel1LData.width = 150;
						cLabel1LData.height = 20;
						uiLevelLabel.setLayoutData(cLabel1LData);
						uiLevelLabel.setLayout(uiLevelLabelLayout);
						uiLevelLabel.setText("Graphische Oberfläche : ");
					}
					{
						uiLevelCombo = new CCombo(individualLoggingComosite, SWT.NONE);
						uiLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						uiLevelCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("uiLevelCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.UI_LOG_LEVEL, uiLevelCombo.getText());
							}
						});
					}
					{
						deviceLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout cLabel1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData cLabel1LData = new RowData();
						cLabel1LData.width = 150;
						cLabel1LData.height = 20;
						deviceLevelLabel.setLayoutData(cLabel1LData);
						deviceLevelLabel.setLayout(cLabel1Layout);
						deviceLevelLabel.setText("Geräte :");
					}
					{
						deviceLevelCombo = new CCombo(individualLoggingComosite, SWT.NONE);
						deviceLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						deviceLevelCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("deviceLevelCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.DEVICE_LOG_LEVEL, deviceLevelCombo.getText());
							}
						});
					}
					{
						commonLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout cLabel2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData cLabel2LData = new RowData();
						cLabel2LData.width = 150;
						cLabel2LData.height = 20;
						commonLevelLabel.setLayoutData(cLabel2LData);
						commonLevelLabel.setLayout(cLabel2Layout);
						commonLevelLabel.setText("Datenmodell : ");
					}
					{
						commonLevelCombo = new CCombo(individualLoggingComosite, SWT.NONE);
						commonLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						commonLevelCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("commonLevelCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.DATA_LOG_LEVEL, commonLevelCombo.getText());
							}
						});
					}
					{
						configLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout cLabel3Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData cLabel3LData = new RowData();
						cLabel3LData.width = 150;
						cLabel3LData.height = 20;
						configLevelLabel.setLayoutData(cLabel3LData);
						configLevelLabel.setLayout(cLabel3Layout);
						configLevelLabel.setText("Konfiguration :");
					}
					{
						configLevelCombo = new CCombo(individualLoggingComosite, SWT.NONE);
						configLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						configLevelCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("configLevelCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.CONFIG_LOG_LEVEL, configLevelCombo.getText());
							}
						});
					}
					{
						utilsLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData cLabel4LData = new RowData();
						cLabel4LData.width = 150;
						cLabel4LData.height = 20;
						utilsLevelLabel.setLayoutData(cLabel4LData);
						utilsLevelLabel.setLayout(cLabel4Layout);
						utilsLevelLabel.setText("allgem. Funktionen : ");
					}
					{
						utilsLevelCombo = new CCombo(individualLoggingComosite, SWT.NONE);
						utilsLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						utilsLevelCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("utilsLevelCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.UTILS_LOG_LEVEL, utilsLevelCombo.getText());
							}
						});
					}
					{
						serialIOLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout cLabel4Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData cLabel4LData = new RowData();
						cLabel4LData.width = 150;
						cLabel4LData.height = 20;
						serialIOLevelLabel.setLayoutData(cLabel4LData);
						serialIOLevelLabel.setLayout(cLabel4Layout);
						serialIOLevelLabel.setText("serial I/O : ");
					}
					{
						serialIOLevelCombo = new CCombo(individualLoggingComosite, SWT.NONE);
						serialIOLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						serialIOLevelCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("serialIOLevelCombo.widgetSelected, event=" + evt);
								settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, serialIOLevelCombo.getText());
							}
						});
					}

				}
			} // end logging group
			{ // begin ok button
				FormData okButtonLData = new FormData();
				okButtonLData.width = 260;
				okButtonLData.height = 25;
				okButtonLData.left =  new FormAttachment(0, 1000, 103);
				okButtonLData.right =  new FormAttachment(1000, 1000, -135);
				okButtonLData.bottom =  new FormAttachment(1000, 1000, -12);
				okButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				okButton.setLayoutData(okButtonLData);
				okButton.setText("OK");
				okButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("okButton.widgetSelected, event=" + evt);
						dialogShell.dispose();
					}
				});
			} // end ok button
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * method to enable / disable log level group
	 */
	private void enableIndividualLogging(boolean value) {
		uiLevelLabel.setEnabled(value);
		uiLevelCombo.setEnabled(value);
		commonLevelLabel.setEnabled(value);
		commonLevelCombo.setEnabled(value);
		deviceLevelLabel.setEnabled(value);
		deviceLevelCombo.setEnabled(value);
		configLevelLabel.setEnabled(value);
		configLevelCombo.setEnabled(value);
		utilsLevelLabel.setEnabled(value);
		utilsLevelCombo.setEnabled(value);
		serialIOLevelLabel.setEnabled(value);
		serialIOLevelCombo.setEnabled(value);
	}

	/**
	 * updates the logging levels in dialog
	 */
	private void updateLoggingLevels() {
		if (settings.getProperty(Settings.GLOBAL_LOG_LEVEL) == null) {
			settings.setProperty(Settings.GLOBAL_LOG_LEVEL, "INFO");
		}
		globalLoggingCombo.setText(settings.getProperty(Settings.GLOBAL_LOG_LEVEL));

		if (settings.getProperty(Settings.UI_LOG_LEVEL) == null) {
			settings.setProperty(Settings.UI_LOG_LEVEL, "INFO");
		}
		uiLevelCombo.setText(settings.getProperty(Settings.UI_LOG_LEVEL));

		if (settings.getProperty(Settings.DATA_LOG_LEVEL) == null) {
			settings.setProperty(Settings.DATA_LOG_LEVEL, "INFO");
		}
		commonLevelCombo.setText(settings.getProperty(Settings.DATA_LOG_LEVEL));

		if (settings.getProperty(Settings.DEVICE_LOG_LEVEL) == null) {
			settings.setProperty(Settings.DEVICE_LOG_LEVEL, "INFO");
		}
		deviceLevelCombo.setText(settings.getProperty(Settings.DEVICE_LOG_LEVEL));

		if (settings.getProperty(Settings.CONFIG_LOG_LEVEL) == null) {
			settings.setProperty(Settings.CONFIG_LOG_LEVEL, "INFO");
		}
		configLevelCombo.setText(settings.getProperty(Settings.CONFIG_LOG_LEVEL));

		if (settings.getProperty(Settings.UTILS_LOG_LEVEL) == null) {
			settings.setProperty(Settings.UTILS_LOG_LEVEL, "INFO");
		}
		utilsLevelCombo.setText(settings.getProperty(Settings.UTILS_LOG_LEVEL));

		if (settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL) == null) {
			settings.setProperty(Settings.SERIAL_IO_LOG_LEVEL, "INFO");
		}
		serialIOLevelCombo.setText(settings.getProperty(Settings.SERIAL_IO_LOG_LEVEL));
	}
}
