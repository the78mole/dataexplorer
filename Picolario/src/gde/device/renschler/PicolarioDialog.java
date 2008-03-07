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
package osde.device.renschler;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
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
 * Dialog class for the Picolariolog device of Uwe Renschler
 * @author Winfried Brügmann
 */
public class PicolarioDialog extends DeviceDialog {
	private Logger										log										= Logger.getLogger(this.getClass().getName());

	private Group											numberAvailableRecorsSetsGroup1;
	private Button										queryAvailableRecordSetButton;
	private CLabel										numberAvailableRecordSetsLabel;
	private String										numberAvailable				= "0";

	private CTabFolder 								configTabFolder;
	private CTabItem									configTabItem1, configTabItem2;
	private PicolarioConfigTab				configTab1, configTab2;

	private Group											readDataGroup3;
	private Button										readSingle;
	private Button closeButton;
	private Button										stopButton;
	private CLabel										alreadyRedLabel;
	private CLabel 										alreadyRedDataSetsLabel;
	private CLabel 										redDataSets;
	private Button										switchRecordSetButton;
	private Button										readAllRecords;
	private CLabel										numberRedTelegramLabel;
	private CCombo										recordSetSelectCombo;
	private String										redDatagrams					= "0";
	private String										redDataSetsText				= "0";
	private boolean 									doSwtichRecordSet = false;

	private final Settings						settings;
	private final Picolario						device;
	private final PicolarioSerialPort	serialPort;
	private OpenSerialDataExplorer		application;
	private DataGathererThread				gatherThread;

	/**
	 * constructor initialize all variables required
	 * @param parent Shell
	 * @param device Picolario class implementation == IDevice
	 */
	public PicolarioDialog(Shell parent, Picolario device) {
		super(parent);
		this.device = device;
		this.serialPort = device.getSerialPort();
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	public void open() {
		log.fine("dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed()));
		if (dialogShell == null || dialogShell.isDisposed()) {
			if (this.settings.isDeviceDialogsModal())
				dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			else
				dialogShell = new Shell(application.getDisplay(), SWT.DIALOG_TRIM);
			
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(344, 591);
			dialogShell.setText("Picolario ToolBox");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
			{
				FormData closeButtonLData = new FormData();
				closeButtonLData.width = 203;
				closeButtonLData.height = 25;
				closeButtonLData.bottom =  new FormAttachment(1000, 1000, -20);
				closeButtonLData.right =  new FormAttachment(1000, 1000, -66);
				closeButtonLData.left =  new FormAttachment(0, 1000, 67);
				closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				closeButton.setLayoutData(closeButtonLData);
				closeButton.setText("Schliessen");
				closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.fine("closeButton.widgetSelected, event="+evt);
						dispose();
					}
				});
			}
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.fine("dialogShell.widgetDisposed, event=" + evt);
					if (gatherThread != null && gatherThread.isAlive()) gatherThread.setThreadStop(true);
				}
			});
			dialogShell.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					configTabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
			});
			{ // group 1
				FormData numberAvailableRecorsSetsGroupLData = new FormData();
			numberAvailableRecorsSetsGroupLData.width = 306;
			numberAvailableRecorsSetsGroupLData.height = 42;
			numberAvailableRecorsSetsGroupLData.left =  new FormAttachment(0, 1000, 12);
			numberAvailableRecorsSetsGroupLData.top =  new FormAttachment(0, 1000, 5);
			numberAvailableRecorsSetsGroupLData.right =  new FormAttachment(1000, 1000, -12);
			numberAvailableRecorsSetsGroup1 = new Group(dialogShell, SWT.NONE);
			numberAvailableRecorsSetsGroup1.setLayout(null);
			numberAvailableRecorsSetsGroup1.setLayoutData(numberAvailableRecorsSetsGroupLData);
			numberAvailableRecorsSetsGroup1.setText("1. Anzahl Aufzeichnungen");
			{
				queryAvailableRecordSetButton = new Button(numberAvailableRecorsSetsGroup1, SWT.PUSH | SWT.CENTER);
				queryAvailableRecordSetButton.setText("Anzahl der Aufzeichnungen auslesen");
				queryAvailableRecordSetButton.setBounds(8, 25, 230, 25);
				queryAvailableRecordSetButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("anzahlAufzeichnungenButton.widgetSelected, event=" + evt);
						try {
							if (serialPort != null) {
								isClosePossible = false;
								int availableRecords = serialPort.readNumberAvailableRecordSets();
								numberAvailable = new Integer(availableRecords).toString();
								numberAvailableRecordSetsLabel.setText(numberAvailable);
								setRecordSetSelection(availableRecords, 0);
								readSingle.setEnabled(true);
								readAllRecords.setEnabled(true);
								resetTelegramLabel();
								resetDataSetsLabel();
								isClosePossible = true;
							}
						}
						catch (Exception e) {
							serialPort.close();
							application.openMessageDialog("Das angeschlossene Gerät antwortet nich auf dem seriellen Port, bitte die Portauswahl überprüfen.");
							application.getDeviceSelectionDialog().open();
						}
					}
				});
			}
			{
				numberAvailableRecordSetsLabel = new CLabel(numberAvailableRecorsSetsGroup1, SWT.RIGHT | SWT.BORDER);
				numberAvailableRecordSetsLabel.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
				numberAvailableRecordSetsLabel.setBounds(255, 25, 29, 22);
			}
			} // end group1
			{ // group 3
				readDataGroup3 = new Group(dialogShell, SWT.NONE);
			readDataGroup3.setLayout(null);
			FormData auslesenGroupLData = new FormData();
			auslesenGroupLData.width = 306;
			auslesenGroupLData.height = 189;
			auslesenGroupLData.left =  new FormAttachment(0, 1000, 12);
			auslesenGroupLData.right =  new FormAttachment(1000, 1000, -12);
			auslesenGroupLData.bottom =  new FormAttachment(1000, 1000, -64);
			readDataGroup3.setLayoutData(auslesenGroupLData);
			readDataGroup3.setText("3. Aufzeichnungen auslesen");
			{
				readSingle = new Button(readDataGroup3, SWT.PUSH | SWT.CENTER);
				readSingle.setText("angewählte Aufzeichnungen auslesen");
				readSingle.setBounds(8, 54, 236, 25);
				readSingle.setEnabled(false);
				readSingle.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("ausleseButton.widgetSelected, event=" + evt);
						isClosePossible = false;
						queryAvailableRecordSetButton.setEnabled(false);
						readSingle.setEnabled(false);
						readAllRecords.setEnabled(false);
						stopButton.setEnabled(true);
						gatherThread = new DataGathererThread(application, device, serialPort, new String[] { recordSetSelectCombo.getText() });
						gatherThread.start();
						log.fine("gatherThread.run() - executing");
					} // end widget selected
				}); // end selection adapter
			}
			{
				readAllRecords = new Button(readDataGroup3, SWT.PUSH | SWT.CENTER);
				readAllRecords.setBounds(8, 112, 292, 25);
				readAllRecords.setText("alle Aufzeichnungen hintereinander auslesen");
				readAllRecords.setEnabled(false);
				readAllRecords.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("readAllRecords.widgetSelected, event=" + evt);
						isClosePossible = false;
						queryAvailableRecordSetButton.setEnabled(false);
						readAllRecords.setEnabled(false);
						readSingle.setEnabled(false);
						stopButton.setEnabled(true);
						String[] itemNames = recordSetSelectCombo.getItems();
						gatherThread = new DataGathererThread(application, device, serialPort, itemNames);
						gatherThread.start();
						log.fine("gatherThread.run() - executing");
					}
				});
			}
			{
				recordSetSelectCombo = new CCombo(readDataGroup3, SWT.BORDER | SWT.RIGHT);
				recordSetSelectCombo.setText("0");
				recordSetSelectCombo.setBounds(252, 56, 45, 22);
			}
			{
				numberRedTelegramLabel = new CLabel(readDataGroup3, SWT.RIGHT);
				numberRedTelegramLabel.setBounds(10, 82, 234, 26);
				numberRedTelegramLabel.setText("Anzahl ausgelesener Telegramme :");
				numberRedTelegramLabel.setForeground(SWTResourceManager.getColor(64, 128, 128));
			}
			{
				alreadyRedLabel = new CLabel(readDataGroup3, SWT.RIGHT);
				alreadyRedLabel.setBounds(244, 82, 56, 26);
				alreadyRedLabel.setText(redDatagrams);
			}
			{
				stopButton = new Button(readDataGroup3, SWT.PUSH | SWT.CENTER);
				stopButton.setText("S T O P");
				stopButton.setEnabled(false);
				stopButton.setBounds(81, 171, 150, 26);
				stopButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("stopButton.widgetSelected, event=" + evt);
						gatherThread.setThreadStop(true);
						isClosePossible = true;
					}
				});
			}
			{
				alreadyRedDataSetsLabel = new CLabel(readDataGroup3, SWT.RIGHT);
				alreadyRedDataSetsLabel.setBounds(10, 139, 234, 26);
				alreadyRedDataSetsLabel.setForeground(SWTResourceManager.getColor(64,128,128));
				alreadyRedDataSetsLabel.setText("aktuelle Datensatznummer :");
			}
			{
				redDataSets = new CLabel(readDataGroup3, SWT.RIGHT);
				redDataSets.setBounds(244, 139, 56, 26);
				redDataSets.setText(redDataSetsText);
			}
			{
				switchRecordSetButton = new Button(readDataGroup3, SWT.CHECK | SWT.CENTER);
				switchRecordSetButton.setBounds(13, 25, 288, 17);
				switchRecordSetButton.setText("Datensatz nach Auslesen sofort anzeigen");
				switchRecordSetButton.setSelection(doSwtichRecordSet);
				switchRecordSetButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("switchRecordSetButton.widgetSelected, event="+evt);
						doSwtichRecordSet = switchRecordSetButton.getSelection();
					}
				});
			}
			} // end group 3
			{
				configTabFolder = new CTabFolder(dialogShell, SWT.BORDER);
				FormData cTabFolder1LData = new FormData(306, 184);
				cTabFolder1LData.width = 306;
				cTabFolder1LData.height = 184;
				cTabFolder1LData.left =  new FormAttachment(0, 1000, 12);
				cTabFolder1LData.right =  new FormAttachment(1000, 1000, -12);
				cTabFolder1LData.top =  new FormAttachment(0, 1000, 72);
				configTabFolder.setLayoutData(cTabFolder1LData);
				{ // config tab
					if(device.getChannelCount() > 0)					if(device.getChannelCount() > 1) {
						configTabItem2 = new CTabItem(configTabFolder, SWT.NONE);
						configTabItem2.setText(device.getChannelName(2));
						configTab2 = new PicolarioConfigTab(configTabFolder, device, device.getChannelName(2));
						configTabItem2.setControl(configTab2);
					}
				{
					configTabItem1 = new CTabItem(configTabFolder, SWT.NONE);
					configTabItem1.setText(device.getChannelName(1));
					configTab1 = new PicolarioConfigTab(configTabFolder, device, device.getChannelName(1));
					configTabItem1.setControl(configTab1);
				}
				}// config tab
				configTabFolder.setSelection(0);
				configTabFolder.setSize(306, 184);
				configTabFolder.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("configTabFolder.widgetSelected, event="+evt);
						int channelNumber = configTabFolder.getSelectionIndex() + 1;
						String configKey = channelNumber + " : " + ((CTabItem)evt.item).getText();
						Channels channels = Channels.getInstance();
						Channel activeChannel = channels.getActiveChannel();
						log.fine("activeChannel = " + activeChannel.getName() + " configKey = " + configKey);
						if (activeChannel != null) {
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet!= null && !activeChannel.getName().trim().equals(configKey)) {
								int answer = application.openYesNoMessageDialog("Soll der aktuelle Datensatz in die selektierte Konfiguration verschoben werden ?");
								if (answer == SWT.YES) {
									System.out.println("verschieben");
									channels.get(channelNumber).put(activeRecordSet.getName(), activeRecordSet.clone(configKey.split(":")[1].trim()));
									activeChannel.remove(activeRecordSet.getName());
									channels.switchChannel(channelNumber);
								}
							}
						}
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
		}
		else {
			dialogShell.setVisible(true);
			dialogShell.setActive();
		}
		Display display = dialogShell.getDisplay();
		while (!dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	public void setAvailableRecordSets(int number) {
		numberAvailableRecordSetsLabel.setText(new Integer(number).toString());
	}

	/**
	 * method to enable setting of selectable values for record set selection
	 * @param items
	 * @param index
	 */
	public void setRecordSetSelection(int items, int index) {
		String[] itemNames = new String[items];
		for (int i = 0; i < items; i++) {
			itemNames[i] = new Integer(i + 1).toString();
		}
		recordSetSelectCombo.setItems(itemNames);
		recordSetSelectCombo.select(index);
		recordSetSelectCombo.setVisibleItemCount(items);
	}

	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetTelegramLabel() {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			redDatagrams = "0";
			alreadyRedLabel.setText(redDatagrams);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					if (!application.getDeviceDialog().isDisposed()) {
						if (!application.getDeviceDialog().isDisposed()) {
							redDatagrams = "0";
							alreadyRedLabel.setText(redDatagrams);
						}
					}
				}
			});
		}
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedText(final int newValue) {
		redDatagrams = new Integer(newValue).toString();
		if (Thread.currentThread().getId() == application.getThreadId()) {
			alreadyRedLabel.setText(redDatagrams);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					if (!application.getDeviceDialog().isDisposed()) alreadyRedLabel.setText(redDatagrams);
				}
			});
		}
	}
	
	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetDataSetsLabel() {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			redDataSetsText = "0";
			redDataSets.setText(redDataSetsText);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					if (!application.getDeviceDialog().isDisposed()) {
						redDataSetsText = "0";
						redDataSets.setText(redDataSetsText);
					}
				}
			});
		}
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedDataSets(final String newValue) {
		redDataSetsText = newValue;
		if (Thread.currentThread().getId() == application.getThreadId()) {
			redDataSets.setText(newValue);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					if (!application.getDeviceDialog().isDisposed()) {
						redDataSets.setText(newValue);
					}
				}
			});
		}
	}

	/**
	 * function to enable all the read data read buttons, normally called after data gathering finished
	 */
	public void enableReadButtons() {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			queryAvailableRecordSetButton.setEnabled(true);
			readSingle.setEnabled(true);
			readAllRecords.setEnabled(true);
			stopButton.setEnabled(false);
			isClosePossible = true;
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					if (!application.getDeviceDialog().isDisposed()) {
						queryAvailableRecordSetButton.setEnabled(true);
						readSingle.setEnabled(true);
						readAllRecords.setEnabled(true);
						stopButton.setEnabled(false);
						isClosePossible = true;
					}
				}
			});
		}
	}
	
	/**
	 * @return the doSwtichRecordSet
	 */
	public boolean isDoSwtichRecordSet() {
		return doSwtichRecordSet;
	}
	
}
