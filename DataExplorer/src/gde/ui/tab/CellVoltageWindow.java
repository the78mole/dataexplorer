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
package osde.ui.tab;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.LithiumValuesDialog;
import osde.utils.LithiumBatteryValues;

/**
 * Display window parent of cellVoltage displays
 * @author Winfried Brügmann
 */
public class CellVoltageWindow {
	final static String					$CLASS_NAME						= CellVoltageWindow.class.getName();
	final static Logger					log										= Logger.getLogger(CellVoltageWindow.$CLASS_NAME);

	Composite										cellVoltageMainComposite, coverComposite;
	Group 											voltageLimitsSelection;
	CTabItem										cellVoltageTab;
	Vector<CellVoltageDisplay>	displays							= new Vector<CellVoltageDisplay>();
	int													voltageAvg						= 0;
	CLabel											infoText;
	Composite										digitalComposite;
	CLabel											capacityUnit;
	CLabel											capacitiyValue;
	CLabel											voltageUnit;
	CLabel											voltageValue;
	Button											liPoButton;
	Button											liIoButton;
	Button											liFeButton;
	Button											individualButton;

	String											info									= Messages.getString(MessageIds.OSDE_MSGT0230);

	final Channels							channels;
	final CTabFolder						displayTab;
	final LithiumValuesDialog		lithiumValuesDialog;

	RecordSet										oldRecordSet					= null;
	Channel											oldChannel						= null;

	// all initial values fit to LiPo akku type
	int[]												voltageLimits					= LithiumBatteryValues.getVoltageLimits();

	class CellInfo { // class to hold voltage and unit information
		final int			voltage;
		final String	name;
		final String	unit;

		CellInfo(int newVoltage, String newName, String newUnit) {
			this.voltage = newVoltage;
			this.name = newName;
			this.unit = newUnit;
		}

		/**
		 * @return the voltage
		 */
		public int getVoltage() {
			return this.voltage;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @return the unit
		 */
		public String getUnit() {
			return this.unit;
		}
	}

	Vector<CellInfo>	voltageVector					= new Vector<CellInfo>();
	int								voltageDelta					= 0;
	Point							displayCompositeSize	= new Point(0, 0);
	boolean 					isUpdateForced 				= false;

	public CellVoltageWindow(CTabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.channels = Channels.getInstance();
		this.lithiumValuesDialog = new LithiumValuesDialog(OpenSerialDataExplorer.getInstance().getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);

	}

	public void create() {
		final String $METHOD_NAME = "create"; //$NON-NLS-1$

		this.cellVoltageTab = new CTabItem(this.displayTab, SWT.NONE);
		this.cellVoltageTab.setText(Messages.getString(MessageIds.OSDE_MSGT0232));
		SWTResourceManager.registerResourceUser(this.displayTab);
		{
			this.cellVoltageMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.cellVoltageTab.setControl(this.cellVoltageMainComposite);
			this.cellVoltageMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINE, "cellVoltageMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
					updateAndResize();
				}
			});
			this.setActiveInfoText(this.info);
			this.infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINE, "infoText.paintControl, event=" + evt); //$NON-NLS-1$
					updateAndResize();
				}
			});

			this.voltageLimitsSelection = new Group(this.cellVoltageMainComposite, SWT.NONE);
			this.voltageLimitsSelection.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.voltageLimitsSelection.setText(Messages.getString(MessageIds.OSDE_MSGT0369));
			this.voltageLimitsSelection.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
			RowLayout thisLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.voltageLimitsSelection.setLayout(thisLayout);
			this.voltageLimitsSelection.setBounds(0, 0, 350, 50);
			this.voltageLimitsSelection.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "voltageLimitsSelection.paintControl, event=" + evt); //$NON-NLS-1$
					CellVoltageWindow.this.liPoButton.setSelection(LithiumBatteryValues.compareVoltageLimits(LithiumBatteryValues.liPoLimits));
					CellVoltageWindow.this.liIoButton.setSelection(LithiumBatteryValues.compareVoltageLimits(LithiumBatteryValues.liIoLimits));
					CellVoltageWindow.this.liFeButton.setSelection(LithiumBatteryValues.compareVoltageLimits(LithiumBatteryValues.liFeLimits));
					CellVoltageWindow.this.individualButton.setSelection(!(CellVoltageWindow.this.liPoButton.getSelection() || CellVoltageWindow.this.liIoButton.getSelection() || CellVoltageWindow.this.liFeButton.getSelection()));
				}
			});
			{
				RowData liPoButtonLData = new RowData();
				liPoButtonLData.width = 70;
				liPoButtonLData.height = 25;
				this.liPoButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.liPoButton.setLayoutData(liPoButtonLData);
				this.liPoButton.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.liPoButton.setText(Messages.getString(MessageIds.OSDE_MSGT0371));
				this.liPoButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
				this.liPoButton.setSelection(false);
				this.liPoButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "buttonLiPo.widgetSelected, event=" + evt); //$NON-NLS-1$
						CellVoltageWindow.this.liPoButton.setSelection(true);
						CellVoltageWindow.this.liIoButton.setSelection(false);
						CellVoltageWindow.this.liFeButton.setSelection(false);
						CellVoltageWindow.this.individualButton.setSelection(false);
						LithiumBatteryValues.setVoltageLimits(LithiumBatteryValues.getLiPoVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						CellVoltageWindow.this.isUpdateForced = true;
						update();
					}
				});
			}
			{
				RowData button1LData = new RowData();
				button1LData.width = 70;
				button1LData.height = 25;
				this.liIoButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.liIoButton.setLayoutData(button1LData);
				this.liIoButton.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.liIoButton.setText(Messages.getString(MessageIds.OSDE_MSGT0372));
				this.liIoButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
				this.liIoButton.setSelection(false);
				this.liIoButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "buttonLiPo.widgetSelected, event=" + evt); //$NON-NLS-1$
						CellVoltageWindow.this.liIoButton.setSelection(true);
						CellVoltageWindow.this.liPoButton.setSelection(false);
						CellVoltageWindow.this.liFeButton.setSelection(false);
						CellVoltageWindow.this.individualButton.setSelection(false);
						LithiumBatteryValues.setVoltageLimits(LithiumBatteryValues.getLiIoVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						CellVoltageWindow.this.isUpdateForced = true;
						update();
					}
				});
			}
			{
				RowData button2LData = new RowData();
				button2LData.width = 70;
				button2LData.height = 25;
				this.liFeButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.liFeButton.setLayoutData(button2LData);
				this.liFeButton.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.liFeButton.setText(Messages.getString(MessageIds.OSDE_MSGT0373));
				this.liFeButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
				this.liFeButton.setSelection(false);
				this.liFeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "buttonLiPo.widgetSelected, event=" + evt); //$NON-NLS-1$
						CellVoltageWindow.this.liFeButton.setSelection(true);
						CellVoltageWindow.this.liPoButton.setSelection(false);
						CellVoltageWindow.this.liIoButton.setSelection(false);
						CellVoltageWindow.this.individualButton.setSelection(false);
						LithiumBatteryValues.setVoltageLimits(LithiumBatteryValues.getLiFeVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						CellVoltageWindow.this.isUpdateForced = true;
						update();
					}
				});
			}
			{
				RowData button1LData1 = new RowData();
				button1LData1.width = 100;
				button1LData1.height = 25;
				this.individualButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.individualButton.setLayoutData(button1LData1);
				this.individualButton.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.individualButton.setText(Messages.getString(MessageIds.OSDE_MSGT0375));
				this.individualButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
				this.individualButton.setSelection(false);
				this.individualButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "buttonLiPo.widgetSelected, event=" + evt); //$NON-NLS-1$
						CellVoltageWindow.this.individualButton.setSelection(true);
						CellVoltageWindow.this.liPoButton.setSelection(false);
						CellVoltageWindow.this.liIoButton.setSelection(false);
						CellVoltageWindow.this.liFeButton.setSelection(false);
						LithiumBatteryValues.setVoltageLimits(new LithiumValuesDialog(OpenSerialDataExplorer.getInstance().getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL).open());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						CellVoltageWindow.this.isUpdateForced = true;
						update();
					}
				});
			}
			this.voltageLimitsSelection.layout();

			this.coverComposite = new Composite(this.cellVoltageMainComposite, SWT.NONE);
			FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
			this.coverComposite.setLayout(fillLayout);

			this.cellVoltageMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.cellVoltageMainComposite.layout();
		}
		{
			this.digitalComposite = new Composite(this.cellVoltageMainComposite, SWT.NONE);
			FillLayout digitalCompositeLayout = new FillLayout(SWT.HORIZONTAL);
			this.digitalComposite.setLayout(digitalCompositeLayout);
			//this.digitalComposite.setBounds(50, 50, 200, 50);
			this.digitalComposite.addPaintListener(new PaintListener() {
				public void paintControl(final PaintEvent evt) {
					log.log(Level.FINEST, "actualDigitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					updateVoltageAndCapacity();
				}
			});
			{
				this.voltageValue = new CLabel(this.digitalComposite, SWT.CENTER);
				this.voltageValue.setText("00.00"); //$NON-NLS-1$
				this.voltageValue.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.voltageValue.setFont(SWTResourceManager.getFont("Sans Serif", 25, SWT.NORMAL)); //$NON-NLS-1$
			}
			{
				this.voltageUnit = new CLabel(this.digitalComposite, SWT.CENTER);
				this.voltageUnit.setText("[V]"); //$NON-NLS-1$
				this.voltageUnit.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.voltageUnit.setFont(SWTResourceManager.getFont("Sans Serif", 18, SWT.NORMAL)); //$NON-NLS-1$
			}
			{
				this.capacitiyValue = new CLabel(this.digitalComposite, SWT.CENTER);
				this.capacitiyValue.setText("0000"); //$NON-NLS-1$
				this.capacitiyValue.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.capacitiyValue.setFont(SWTResourceManager.getFont("Sans Serif", 25, SWT.NORMAL)); //$NON-NLS-1$
			}
			{
				this.capacityUnit = new CLabel(this.digitalComposite, SWT.CENTER);
				this.capacityUnit.setText("[mAh]"); //$NON-NLS-1$
				this.capacityUnit.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.capacityUnit.setFont(SWTResourceManager.getFont("Sans Serif", 18, SWT.NORMAL)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String updateInfo) {
		if (this.infoText == null || this.infoText.isDisposed()) {
			this.infoText = new CLabel(this.cellVoltageMainComposite, SWT.CENTER);
			this.infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			this.infoText.setBounds(0, 0, 350, 50);
			this.infoText.setText(updateInfo);
			this.infoText.redraw(0, 0, 350, 50, true);
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds() {
		updateCellVoltageVector();
		updateVoltageAndCapacity();
		log.log(Level.FINER, "voltageValues.length = " + this.voltageVector.size() + " displays.size() = " + this.displays.size()); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.voltageVector.size() > 0 && this.voltageVector.size() == this.displays.size()) { // channel does not have a record set yet
			this.voltageDelta = calculateVoltageDelta(this.voltageVector);
			for (int i = 0; i < this.voltageVector.size(); ++i) {
				this.displays.get(i).setVoltage(this.voltageVector.get(i).getVoltage());
				this.displays.get(i).redraw();
				log.log(Level.FINE, "setVoltage cell " + i + " - " + this.voltageVector.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			update();
		}
	}

	/**
	 * method to update cellVoltage window by adding removing cellVoltage displays
	 */
	public void update() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested() && !this.voltageVector.isEmpty()) {

				updateCellVoltageVector();

				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName()) || this.oldChannel == null
						|| !this.oldChannel.getName().equals(activeChannel.getName()) || this.displays.size() != this.voltageVector.size() || this.isUpdateForced;

				log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
				if (isUpdateRequired) {
					// remove into text 
					if (!this.infoText.isDisposed()) this.infoText.dispose();
					// cleanup
					for (CellVoltageDisplay display : this.displays) {
						if (display != null) {
							if (!display.isDisposed()) {
								display.dispose();
								display = null;
							}
						}
					}
					this.displays.removeAllElements();
					// add new
					for (int i = 0; this.voltageVector != null && i < this.voltageVector.size(); ++i) {
						CellVoltageDisplay display = new CellVoltageDisplay(this.coverComposite, this.voltageVector.get(i).getVoltage(), this.voltageVector.get(i).getName(), this.voltageVector.get(i).getUnit(), this);
						display.create();
						log.log(Level.FINER, "created cellVoltage display for " + this.voltageVector.get(i).getVoltage()); //$NON-NLS-1$
						this.displays.add(display);
					}
					this.oldRecordSet = recordSet;
					this.oldChannel = activeChannel;
					this.updateChilds();
				}
			}
			else { // clean up after device switched
				for (CellVoltageDisplay display : this.displays) {
					if (display != null) {
						if (!display.isDisposed()) {
							display.dispose();
							display = null;
						}
					}
				}
				this.displays.removeAllElements();
				if (recordSet != null && (!recordSet.getDevice().isVoltagePerCellTabRequested() || this.voltageVector.isEmpty())) {
					if (this.infoText.isDisposed())
						this.setActiveInfoText(this.info);
					else
						this.infoText.setText(this.info);
				}
			}
			this.cellVoltageMainComposite.layout();
			this.coverComposite.layout();
		}
		this.isUpdateForced = false;
	}

	/**
	 * check cell voltage availability and build cell voltage array
	 */
	void updateCellVoltageVector() {
		this.voltageVector = new Vector<CellInfo>();
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested()) {
				int cellCount = this.voltageAvg = 0;
				String[] activeRecordKeys = recordSet.getSyncableRecords().toArray(new String[0]);
				for (String recordKey : activeRecordKeys) {
					Record record = recordSet.get(recordKey);
					int index = record.getName().length();
					//log.log(Level.FINER, "record " + record.getName() + " symbol " + record.getSymbol() + " - " + record.getName().substring(index-1, index));
					// algorithm to check if a measurement is a single cell voltage is check match of last character symbol and name U1-Voltage1
					if (record.getSymbol().endsWith(record.getName().substring(index - 1))) { // better use a propperty to flag as single cell voltage
						if (record.getLast() > 0) { // last value is current value
							this.voltageVector.add(new CellInfo(record.getLast(), record.getName(), record.getUnit()));
							this.voltageAvg += record.getLast();
							cellCount++;
						}
						//log.log(Level.INFO, "record.getLast() " + record.getLast());
					}
				}
				// add test values here
				//cellCount = addCellVoltages4Test(new int[] {2500, 3500, 3200, 4250}, "ZellenSpannung");
				//cellCount = addCellVoltages4Test(new int[] {2500, 3500, 3200, 4250}, "CellVoltage");
				//cellCount = addCellVoltages4Test(new int[] {4120, 4150, 4175, 4200}, "ZellenSpannung");
				//cellCount = addCellVoltages4Test(new int[] {4120, 4150, 4175, 4200}, "CellVoltage");

				if (cellCount > 0) this.voltageAvg = this.voltageAvg / cellCount;
				//log.log(Level.INFO, "cellCount  = " + cellCount + " cell voltage average = " + this.voltageAvg);
			}
		}
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			for (CellInfo cellInfo : this.voltageVector) {
				sb.append(cellInfo.getVoltage()).append(" "); //$NON-NLS-1$
			}
			log.log(Level.FINE, "updateCellVoltageVector -> " + sb.toString()); //$NON-NLS-1$
		}
	}

	/**
	 * add test voltage values for test and to create sceenshots for documentation
	 * @param values array with dummy cell voltages
	 * @return
	 */
	int addCellVoltages4Test(int[] values, String measurementName) {
		this.voltageVector = new Vector<CellInfo>();
		for (int i = 0; i < values.length; i++) {
			this.voltageVector.add(new CellInfo(values[i], measurementName + (i + 1), "V")); //$NON-NLS-1$
		}
		return values.length;
	}

	/**
	 * calculates the voltage delta over all given cell voltages
	 * @param newValues
	 */
	private int calculateVoltageDelta(Vector<CellInfo> newValues) {
		int min = newValues.firstElement().getVoltage();
		int max = newValues.firstElement().getVoltage();
		for (CellInfo cellInfo : newValues) {
			if (cellInfo.voltage < min)
				min = cellInfo.voltage;
			else if (cellInfo.voltage > max) max = cellInfo.voltage;
		}
		return max - min;
	}

	/**
	 * @return the voltageDelta
	 */
	public int getVoltageDelta() {
		return this.voltageDelta;
	}

	/**
	 * @return the displayCompositeSize
	 */
	public Point getDisplayCompositeSize() {
		return this.displayCompositeSize;
	}

	/**
	 * 
	 */
	void updateAndResize() {
		updateCellVoltageVector();
		Point mainSize = CellVoltageWindow.this.cellVoltageMainComposite.getSize();
		if (this.voltageVector.size() > 0) {
			//log.log(Level.INFO, "mainSize = " + mainSize.toString());
			int cellWidth = mainSize.x / 6;
			int x = (6 - CellVoltageWindow.this.voltageVector.size()) * cellWidth / 2;
			int width = mainSize.x - (2 * x);
			Rectangle bounds = new Rectangle(x, mainSize.y * 10 / 100, width, mainSize.y * 80 / 100);
			//log.log(Level.INFO, "cover bounds = " + bounds.toString());
			CellVoltageWindow.this.coverComposite.setBounds(bounds);
			CellVoltageWindow.this.digitalComposite.setBounds((mainSize.x - 350) / 2, mainSize.y * 90 / 100, 350, 50);

		}
		else {
			CellVoltageWindow.this.coverComposite.setSize(0, 0);
			clearVoltageAndCapacity();
		}
		update();
	}

	/**
	 * @return the cellVoltageMainComposite
	 */
	public Composite getCellVoltageMainComposite() {
		return this.cellVoltageMainComposite;
	}

	/**
	 * @return the voltageAvg
	 */
	public int getVoltageAvg() {
		return this.voltageAvg;
	}

	/**
	 * 
	 */
	void updateVoltageAndCapacity() {
		Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
		IDevice device = OpenSerialDataExplorer.getInstance().getActiveDevice();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				String[] recordKeys = activeRecordSet.getActiveRecordNames();
				Record record_U = activeRecordSet.getRecord(recordKeys[0]); // voltage U
				Record record_C = activeRecordSet.getRecord(recordKeys[2]); // capacitiy C
				if (record_U != null && record_C != null) {
					CellVoltageWindow.this.voltageValue.setForeground(record_U.getColor());
					CellVoltageWindow.this.voltageValue.setText(record_U.getDecimalFormat().format(device.translateValue(record_U, new Double(record_U.getLast() / 1000.0))));
					CellVoltageWindow.this.voltageUnit.setText("[" + record_U.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
					CellVoltageWindow.this.capacitiyValue.setForeground(record_C.getColor());
					CellVoltageWindow.this.capacitiyValue.setText(record_C.getDecimalFormat().format(device.translateValue(record_C, new Double(record_C.getLast() / 1000.0))));
					CellVoltageWindow.this.capacityUnit.setText("[" + record_C.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			else {
				clearVoltageAndCapacity();
			}
		}
		else {
			clearVoltageAndCapacity();
		}
	}

	/**
	 * 
	 */
	void clearVoltageAndCapacity() {
		CellVoltageWindow.this.voltageValue.setText(""); //$NON-NLS-1$
		CellVoltageWindow.this.voltageUnit.setText(""); //$NON-NLS-1$
		CellVoltageWindow.this.capacitiyValue.setText(""); //$NON-NLS-1$
		CellVoltageWindow.this.capacityUnit.setText(""); //$NON-NLS-1$
	}
}
