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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import osde.device.DeviceDialog;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

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

	private Group											heightAdaptionGroup2;
	private Button										reduceByFirstValueButton;
	private CCombo										heightUnit;
	private Label											heightUnitLabel;
	private CCombo										heightOffset;
	private Button										noAdaptioButton;
	private Button										reduceByDefinedValueButton;
	private Label											heightReductionLabel;
	private boolean										doSubtractFirst				= true;																					// indicates to subtract first values from all other
	private boolean										doSubtractLast				= false;																				// indicates to subtract last values from all other
	private boolean										doReduceHeight				= false;																				// indicates that the height has to be corrected by an offset
	private int												heightUnitSelection		= 0;																						// Feet 0, Meter 1
	private int												heightOffsetSelection	= 7;																						// represents the offset the measurment should be modified
	private double										heightOffsetValue			= 100;																					// represents the offset value
	private boolean 									doSwtichRecordSet = false;

	private Group											readDataGroup3;
	private Button										readSingle;
	private Button										stopButton;
	private CLabel										alreadyRedLabel;
	private CLabel 										alreadyRedDataSetsLabel;
	private CLabel 										redDataSets;
	private Button										switchRecordSetButton;
	private Button										reduceByLastValueButton;
	private Button										readAllRecords;
	private CLabel										numberRedTelegramLabel;
	private CCombo										recordSetSelectCombo;
	private String										redDatagrams					= "0";
	private String										heightDataUnit				= "m";																					// Meter is default
	private String										redDataSetsText				= "0";

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
	}

	public void open() {
		log.fine("dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed()));
		if (dialogShell == null || dialogShell.isDisposed()) {
			dialogShell = new Shell(new Shell(SWT.MODELESS), SWT.DIALOG_TRIM);
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(344, 490);
			dialogShell.setText("Picolario ToolBox");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.fine("dialogShell.widgetDisposed, event=" + evt);
					if (gatherThread != null && gatherThread.isAlive()) gatherThread.setThreadStop(true);
				}
			});
			{ // group 1
				FormData numberAvailableRecorsSetsGroupLData = new FormData();
				numberAvailableRecorsSetsGroupLData.width = 306;
				numberAvailableRecorsSetsGroupLData.height = 42;
				numberAvailableRecorsSetsGroupLData.left = new FormAttachment(0, 1000, 12);
				numberAvailableRecorsSetsGroupLData.top = new FormAttachment(0, 1000, 12);
				numberAvailableRecorsSetsGroupLData.right = new FormAttachment(1000, 1000, -12);
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
								if (serialPort != null && serialPort.isConnected()) {
									int availableRecords = serialPort.readNumberAvailableRecordSets();
									numberAvailable = new Integer(availableRecords).toString();
									numberAvailableRecordSetsLabel.setText(numberAvailable);
									setRecordSetSelection(availableRecords, 0);
									readSingle.setEnabled(true);
									readAllRecords.setEnabled(true);
									resetTelegramLabel();
									resetDataSetsLabel();
								}
								else
									application.openMessageDialog("Erst den seriellen Port öffnen");
							}
							catch (Exception e) {
								serialPort.close();
								application.openMessageDialog("Das angeschlossene Gerät antwortet nich auf dem seriellen Port");
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
				auslesenGroupLData.height = 196;
				auslesenGroupLData.left =  new FormAttachment(0, 1000, 12);
				auslesenGroupLData.right =  new FormAttachment(1000, 1000, -12);
				auslesenGroupLData.bottom =  new FormAttachment(1000, 1000, -7);
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
					stopButton.setBounds(74, 176, 150, 26);
					stopButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("stopButton.widgetSelected, event=" + evt);
							gatherThread.setThreadStop(true);
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
			{ // group 2
				heightAdaptionGroup2 = new Group(dialogShell, SWT.NONE);
				heightAdaptionGroup2.setLayout(null);
				FormData heightAdaptionGroupLData = new FormData();
				heightAdaptionGroupLData.width = 306;
				heightAdaptionGroupLData.height = 119;
				heightAdaptionGroupLData.left =  new FormAttachment(0, 1000, 12);
				heightAdaptionGroupLData.top =  new FormAttachment(0, 1000, 85);
				heightAdaptionGroupLData.right =  new FormAttachment(1000, 1000, -12);
				heightAdaptionGroup2.setLayoutData(heightAdaptionGroupLData);
				heightAdaptionGroup2.setText("2. Einstellungen für die Höhenberechnung");
				{
					noAdaptioButton = new Button(heightAdaptionGroup2, SWT.RADIO | SWT.LEFT);
					noAdaptioButton.setText("Höhenwerte nicht anpassen");
					noAdaptioButton.setBounds(12, 21, 186, 16);
					noAdaptioButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("noAdaptioButton.widgetSelected, event=" + evt);
							noAdaptioButton.setSelection(true);
							reduceByFirstValueButton.setSelection(false);
							reduceByLastValueButton.setSelection(false);
							reduceByDefinedValueButton.setSelection(false);
							doSubtractFirst = false;
							doSubtractLast = false;
							doReduceHeight = false;
							application.updateGraphicsWindow();
						}
					});
				}
				{
					reduceByFirstValueButton = new Button(heightAdaptionGroup2, SWT.RADIO | SWT.LEFT);
					reduceByFirstValueButton.setText("ersten Höhenwert von den folgenden abziehen");
					reduceByFirstValueButton.setSelection(doSubtractFirst);
					reduceByFirstValueButton.setBounds(12, 42, 297, 16);
					reduceByFirstValueButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("reduceByFirstValueButton.widgetSelected, event=" + evt);
							noAdaptioButton.setSelection(false);
							reduceByFirstValueButton.setSelection(true);
							reduceByLastValueButton.setSelection(false);
							reduceByDefinedValueButton.setSelection(false);
							doSubtractFirst = true;
							doSubtractLast = false;
							doReduceHeight = false;
							application.updateGraphicsWindow();
						}
					});
				}
				{
					reduceByLastValueButton = new Button(heightAdaptionGroup2, SWT.RADIO | SWT.LEFT);
					reduceByLastValueButton.setBounds(12, 62, 293, 18);
					reduceByLastValueButton.setText("letzten Höhenwert von allen anderen abziehen");
					reduceByLastValueButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("reduceByLastValueButton.widgetSelected, event=" + evt);
							noAdaptioButton.setSelection(false);
							reduceByFirstValueButton.setSelection(false);
							reduceByLastValueButton.setSelection(true);
							reduceByDefinedValueButton.setSelection(false);
							doSubtractFirst = false;
							doSubtractLast = true;
							doReduceHeight = false;
							application.updateGraphicsWindow();
						}
					});
				}
				{
					reduceByDefinedValueButton = new Button(heightAdaptionGroup2, SWT.RADIO | SWT.LEFT);
					reduceByDefinedValueButton.setText("Höhe um");
					reduceByDefinedValueButton.setSelection(doReduceHeight);
					reduceByDefinedValueButton.setBounds(12, 84, 75, 16);
					reduceByDefinedValueButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("reduceByDefinedValueButton.widgetSelected, event=" + evt);
							noAdaptioButton.setSelection(false);
							reduceByFirstValueButton.setSelection(false);
							reduceByLastValueButton.setSelection(false);
							reduceByDefinedValueButton.setSelection(true);
							doReduceHeight = true;
							doSubtractFirst = false;
							doSubtractLast = false;
							heightOffsetValue = new Double(heightOffset.getText()).doubleValue();
							application.updateGraphicsWindow();
						}
					});
				}
				{
					heightOffset = new CCombo(heightAdaptionGroup2, SWT.BORDER);
					final String[] heightOffsetValues = new String[] {"-200", "-100", "-50", "0", "50", "100", "150", "200", "250", "300", "400", "500", "750", "1000", "1500" };
					heightOffset.setItems(heightOffsetValues);
					heightOffset.setText(new Double(heightOffsetValue).toString());
					for (int i = 0; i < heightOffsetValues.length; i++) {
						if (heightOffsetValues.equals(heightOffsetValue)) heightOffset.select(heightOffsetSelection);
					}
					heightOffset.setBounds(104, 84, 84, 21);
					heightOffset.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("heightOffset.widgetSelected, event=" + evt);
							heightOffsetValue = new Double(heightOffsetValues[heightOffset.getSelectionIndex()]).doubleValue();
							application.updateGraphicsWindow();
						}
					});
					heightOffset.addKeyListener(new KeyAdapter() {
						public void keyPressed(KeyEvent evt) {
							log.finest("heightOffset.keyPressed, event=" + evt);
							if (evt.character == SWT.CR) {
								//heightOffsetSelection 
								try {
									heightOffsetValue = new Double(heightOffset.getText().replace(',', '.')).doubleValue();
									application.updateGraphicsWindow();
								}
								catch (NumberFormatException e) {
									log.log(Level.WARNING, e.getMessage(), e);
									application.openMessageDialog("Eingabefehler : " + e.getMessage());
								}
							}
						}
					});
				}
				{
					heightReductionLabel = new Label(heightAdaptionGroup2, SWT.NONE);
					heightReductionLabel.setBounds(197, 82, 60, 19);
					heightReductionLabel.setText("verringern");
				}
				{
					heightUnitLabel = new Label(heightAdaptionGroup2, SWT.NONE);
					heightUnitLabel.setBounds(14, 108, 88, 19);
					heightUnitLabel.setText("Höheneinheit");
				}
				{
					heightUnit = new CCombo(heightAdaptionGroup2, SWT.BORDER | SWT.LEFT);
					heightUnit.setBounds(104, 108, 84, 21);
					heightUnit.setItems(new java.lang.String[] { "Meter", "Fuß" });
					heightUnit.setEditable(false);
					heightUnit.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					heightUnit.setToolTipText("1 Meter = 3.2808 Fuß  <-->  1 Fuß = 0.3048 Meter]");
					heightUnit.select(heightUnitSelection); // feet is default
					heightUnit.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("höhenMassCombo.widgetSelected, event=" + evt);
							heightUnitSelection = heightUnit.getSelectionIndex();
							switch (heightUnitSelection) {
							case 0:
								heightDataUnit = "m";
								break;
							case 1:
								heightDataUnit = "Fuß";
								break;
							}
							application.updateGraphicsWindow();
							application.updateDigitalWindowChilds();
							application.updateDataTable();
						}
					});
				}
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
	 * returns the check info as follow
	 *   1 = nichtAnpassenButton.setSelection(true);
	 *   2 = ertsenWertAbziehenButton.setSelection(true);
	 *   4 = höheVerringernButton.setSelection(true);
	 *   only 1 or 2 or 4 can returned
	 * @return integer of unit type
	 */
	public int getReduceHeightSelectionType() {
		int selection = noAdaptioButton.getSelection() ? 1 : 0;
		selection = selection + (reduceByFirstValueButton.getSelection() ? 1 : 0);
		selection = selection + (reduceByDefinedValueButton.getSelection() ? 1 : 0);
		return selection;
	}

	/**
	 * call this method, if reduceHeightSelectiontYpe is 4
	 * @return integer of dialog selected value
	 */
	public int getReduceHeightSelection() {
		return new Integer(heightOffset.getText()).intValue();
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
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				redDatagrams = "0";
				alreadyRedLabel.setText(redDatagrams);
			}
		});
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedText(final int newValue) {
		redDatagrams = new Integer(newValue).toString();
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				alreadyRedLabel.setText(redDatagrams);
			}
		});
	}
	
	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetDataSetsLabel() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				redDataSetsText = "0";
				redDataSets.setText(redDataSetsText);
			}
		});
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedDataSets(final String newValue) {
		redDataSetsText = newValue;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				redDataSets.setText(newValue);
			}
		});
	}

	/**
	 * function to enable all the read data read buttons, normally called after data gathering finished
	 */
	public void enableReadButtons() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				queryAvailableRecordSetButton.setEnabled(true);
				readSingle.setEnabled(true);
				readAllRecords.setEnabled(true);
				stopButton.setEnabled(false);
			}
		});
	}

	/**
	 * @return the heightDataUnit
	 */
	public String getHeightDataUnit() {
		return heightDataUnit;
	}

	/**
	 * @return the heightUnitSelection
	 */
	public int getHeightUnitSelection() {
		return heightUnitSelection;
	}

	/**
	 * @return the heightOffsetValue
	 */
	public double getHeightOffsetValue() {
		return heightOffsetValue;
	}

	/**
	 * @return the doReduceHeight
	 */
	public boolean isDoReduceHeight() {
		return doReduceHeight;
	}

	/**
	 * @return the doSubtractFirst
	 */
	public boolean isDoSubtractFirst() {
		return doSubtractFirst;
	}

	/**
	 * @return the doSubtractLast
	 */
	public boolean isDoSubtractLast() {
		return doSubtractLast;
	}

	/**
	 * @return the doSwtichRecordSet
	 */
	public boolean isDoSwtichRecordSet() {
		return doSwtichRecordSet;
	}
}
