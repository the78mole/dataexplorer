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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
/**
 * Dialog class to adjust application wide properties
 * @author Winfried Brügmann
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
	private Button deviceDialogButton;
	private Group deviceDialogGroup;
	private Group													serialPortGroup;
	private Group													separatorGroup;
	private CCombo												listSeparator;
	private CLabel												listSeparatorLabel;
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
			dialogShell.setSize(496, 575);
			dialogShell.setText("OpenSerialDataExplorer - Settings");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.gif"));
			dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					System.out.println("dialogShell.helpRequested, event="+evt);
					//TODO add your code for dialogShell.helpRequested
				}
			});
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
			{
				FormData GerätedialogLData = new FormData();
				GerätedialogLData.width = 451;
				GerätedialogLData.height = 35;
				GerätedialogLData.left =  new FormAttachment(0, 1000, 12);
				GerätedialogLData.top =  new FormAttachment(0, 1000, 153);
				deviceDialogGroup = new Group(dialogShell, SWT.NONE);
				deviceDialogGroup.setLayout(null);
				deviceDialogGroup.setLayoutData(GerätedialogLData);
				deviceDialogGroup.setText("Gerätedialoge");
				deviceDialogGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("deviceDialogGroup.paintControl, event="+evt);
						deviceDialogButton.setSelection(settings.isDeviceDialogsModal());
					}
				});
				{
					deviceDialogButton = new Button(deviceDialogGroup, SWT.CHECK | SWT.LEFT);
					deviceDialogButton.setText("    Gerätedialoge anwendungsmodal einstellen");
					deviceDialogButton.setBounds(65, 24, 327, 16);
					deviceDialogButton.setToolTipText("Hiermit stellt man ein ob die Gerätedialoge erst geschlosen werden müssen bevor man an die Hauptfenster herankommt");
					deviceDialogButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("deviceDialogButton.widgetSelected, event="+evt);
							settings.enabelModalDeviceDialogs(deviceDialogButton.getSelection());
						}
					});
				}
			}
			{ // begin default data path group
				defaultDataPathGroup = new Group(dialogShell, SWT.NONE);
				defaultDataPathGroup.setLayout(null);
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
					defaultDataPathLabel.setText("Datenpfad  :    ");
					defaultDataPathLabel.setBounds(14, 24, 90, 20);
				}
				{
					defaultDataPath = new Text(defaultDataPathGroup, SWT.BORDER);
					defaultDataPath.setBounds(107, 24, 295, 20);
				}
				{
					defaultDataPathAdjustButton = new Button(defaultDataPathGroup, SWT.PUSH | SWT.CENTER);
					defaultDataPathAdjustButton.setText(". . . ");
					defaultDataPathAdjustButton.setBounds(405, 24, 30, 20);
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
				separatorGroup.setLayout(null);
				FormData separatorGroupLData = new FormData();
				separatorGroupLData.width = 451;
				separatorGroupLData.height = 44;
				separatorGroupLData.left =  new FormAttachment(0, 1000, 12);
				separatorGroupLData.top =  new FormAttachment(0, 1000, 80);
				separatorGroupLData.right =  new FormAttachment(1000, 1000, -19);
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
					decimalSeparatorLabel.setBounds(28, 24, 122, 22);
				}
				{
					decimalSeparator = new CCombo(separatorGroup, SWT.BORDER);
					decimalSeparator.setItems(new String[] { " . ", " , " });
					decimalSeparator.setBounds(153, 24, 43, 20);
					decimalSeparator.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("decimalSeparator.widgetSelected, event=" + evt);
							settings.setDecimalSeparator(decimalSeparator.getText().trim());
							decimalSeparator.setText(" " + decimalSeparator.getText().trim() + " ");
						}
					});
				}
				{
					listSeparatorLabel = new CLabel(separatorGroup, SWT.NONE);
					listSeparatorLabel.setText("Listenseparator : ");
					listSeparatorLabel.setToolTipText("Der Listenseparator is abhängig von den eingestellten Systemlocalen, einige Betribssysteme erlauben aber davon abweichende Konfiguration");
					listSeparatorLabel.setBounds(258, 24, 108, 22);
				}
				{
					listSeparator = new CCombo(separatorGroup, SWT.BORDER);
					listSeparator.setItems(new String[] { " , ", " ; " });
					listSeparator.setBounds(369, 24, 47, 20);
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
				serialPortGroup.setLayout(null);
				FormData serialPortGroupLData = new FormData();
				serialPortGroupLData.width = 451;
				serialPortGroupLData.height = 34;
				serialPortGroupLData.left =  new FormAttachment(0, 1000, 12);
				serialPortGroupLData.right =  new FormAttachment(1000, 1000, -19);
				serialPortGroupLData.top =  new FormAttachment(0, 1000, 216);
				serialPortGroup.setLayoutData(serialPortGroupLData);
				serialPortGroup.setText("Serial Port Adjustment");
				serialPortGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("serialPortGroup.paintControl, event=" + evt);
						useGlobalSerialPort.setSelection(settings.isGlobalSerialPort());
						serialPort.setText(settings.getSerialPort());
						// execute independent from dialog UI
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
					useGlobalSerialPort.setToolTipText("Steht dieser Schalter angewählt wird Anwendungsweit nur ein serieller Port verwendet, sonst wird pro Gerät eine eigene Portkonfiguration verwendet");
					useGlobalSerialPort.setBounds(15, 19, 251, 20);
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
					serialPort = new CCombo(serialPortGroup, SWT.BORDER);
					serialPort.setBounds(269, 19, 84, 20);
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
					Port.setBounds(356, 19, 81, 22);
				}
			} // end serial port group
			{ // begin logging group
				loggingGroup = new Group(dialogShell, SWT.NONE);
				loggingGroup.setLayout(null);
				FormData loggingGroupLData = new FormData();
				loggingGroupLData.width = 451;
				loggingGroupLData.height = 195;
				loggingGroupLData.left =  new FormAttachment(0, 1000, 12);
				loggingGroupLData.top =  new FormAttachment(0, 1000, 276);
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

					globalLoggingComposite = new Composite(loggingGroup, SWT.NONE);
					globalLoggingComposite.setLayout(null);
					globalLoggingComposite.setBounds(6, 19, 154, 50);
					{
						globalLogLevel = new Button(globalLoggingComposite, SWT.CHECK | SWT.LEFT);

						globalLogLevel.setText(" globaler Log Level");
						globalLogLevel.setBounds(4, 3, 148, 21);
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
						globalLoggingCombo = new CCombo(globalLoggingComposite, SWT.BORDER);
						globalLoggingCombo.setItems(new String[] { "SEVERE", "WARNING", "INFO", "FINE", "FINER", "FINEST" });
						globalLoggingCombo.setBounds(4, 28, 148, 21);
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
					individualLoggingComosite.setLayout(null);
					individualLoggingComosite.setBounds(172, 19, 278, 184);
					{
						uiLevelLabel = new CLabel(individualLoggingComosite, SWT.NONE);
						RowLayout uiLevelLabelLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);

						uiLevelLabel.setLayout(uiLevelLabelLayout);
						uiLevelLabel.setText("Graphische Oberfläche : ");
						uiLevelLabel.setBounds(3, 3, 170, 20);
					}
					{
						uiLevelCombo = new CCombo(individualLoggingComosite, SWT.BORDER);
						uiLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						uiLevelCombo.setBounds(183, 3, 79, 21);
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

						deviceLevelLabel.setLayout(cLabel1Layout);
						deviceLevelLabel.setText("Geräte :");
						deviceLevelLabel.setBounds(3, 27, 170, 20);
					}
					{
						deviceLevelCombo = new CCombo(individualLoggingComosite, SWT.BORDER);
						deviceLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						deviceLevelCombo.setBounds(183, 27, 79, 21);
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

						commonLevelLabel.setLayout(cLabel2Layout);
						commonLevelLabel.setText("Datenmodell : ");
						commonLevelLabel.setBounds(3, 51, 170, 20);
					}
					{
						commonLevelCombo = new CCombo(individualLoggingComosite, SWT.BORDER);
						commonLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						commonLevelCombo.setBounds(183, 51, 79, 21);
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

						configLevelLabel.setLayout(cLabel3Layout);
						configLevelLabel.setText("Konfiguration :");
						configLevelLabel.setBounds(3, 75, 170, 20);
					}
					{
						configLevelCombo = new CCombo(individualLoggingComosite, SWT.BORDER);
						configLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						configLevelCombo.setBounds(183, 75, 79, 21);
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

						utilsLevelLabel.setLayout(cLabel4Layout);
						utilsLevelLabel.setText("allgem. Funktionen : ");
						utilsLevelLabel.setBounds(3, 99, 170, 20);
					}
					{
						utilsLevelCombo = new CCombo(individualLoggingComosite, SWT.BORDER);
						utilsLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						utilsLevelCombo.setBounds(183, 99, 79, 21);
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

						serialIOLevelLabel.setLayout(cLabel4Layout);
						serialIOLevelLabel.setText("serial I/O : ");
						serialIOLevelLabel.setBounds(3, 123, 170, 20);
					}
					{
						serialIOLevelCombo = new CCombo(individualLoggingComosite, SWT.BORDER);
						serialIOLevelCombo.setItems(new String[] { "INFO", "FINE", "FINER", "FINEST" });
						serialIOLevelCombo.setBounds(183, 123, 79, 21);
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
				okButtonLData.width = 250;
				okButtonLData.height = 25;
				okButtonLData.left =  new FormAttachment(0, 1000, 116);
				okButtonLData.right =  new FormAttachment(1000, 1000, -122);
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
