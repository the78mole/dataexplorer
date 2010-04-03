/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.tab;

import java.text.DecimalFormat;
import java.util.Vector;
import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.CellVoltageValuesDialog;
import osde.ui.menu.TabAreaContextMenu;
import osde.utils.CellVoltageValues;

/**
 * Display window parent of cellVoltage displays
 * @author Winfried Brügmann
 */
public class CellVoltageWindow extends CTabItem {
	final static String					$CLASS_NAME						= CellVoltageWindow.class.getName();
	final static Logger					log										= Logger.getLogger(CellVoltageWindow.$CLASS_NAME);

	Composite										cellVoltageMainComposite, coverComposite;
	Group 											voltageLimitsSelection;
	Composite										digitalComposite;
	CLabel											capacityUnit;
	CLabel											capacitiyValue;
	CLabel											voltageUnit;
	CLabel											voltageValue;
	Button											liPoButton;
	Button											liIoButton;
	Button											liFeButton;
	Button											niMhButton;
	Button											individualButton;

	final DataExplorer	application;
	final Channels								channels;
	final CTabFolder							displayTab;
	final CellVoltageValuesDialog	lithiumValuesDialog;
	final Menu										popupmenu;
	final TabAreaContextMenu			contextMenu;

	RecordSet										oldRecordSet					= null;
	Channel											oldChannel						= null;
	Color												surroundingBackground;
	String											info									= Messages.getString(MessageIds.OSDE_MSGT0230);
	Vector<CellVoltageDisplay>	displays							= new Vector<CellVoltageDisplay>();
	int													voltageAvg						= 0;

	// all initial values fit to LiPo akku type
	int[]												voltageLimits					= CellVoltageValues.getVoltageLimits();

	static class CellInfo { // class to hold voltage and unit information
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
	int								firstMeasurement 			= 0; // total battery voltage
	int								secondMeasurement 		= 2; // charged /discharged capacity

	public CellVoltageWindow(CTabFolder currentDisplayTab, int style, int position) {
		super(currentDisplayTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.displayTab = currentDisplayTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.OSDE_MSGT0232));
	
		this.lithiumValuesDialog = new CellVoltageValuesDialog(this.application, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.surroundingBackground = Settings.getInstance().getCellVoltageSurroundingAreaBackground();
	}

	public void create() {
		final String $METHOD_NAME = "create"; //$NON-NLS-1$
		{
			this.cellVoltageMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.setControl(this.cellVoltageMainComposite);
			this.cellVoltageMainComposite.setMenu(this.popupmenu);
			this.cellVoltageMainComposite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "cellVoltageMainComposite.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_9.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.cellVoltageMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "cellVoltageMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
					CellVoltageWindow.this.contextMenu.createMenu(CellVoltageWindow.this.popupmenu, TabAreaContextMenu.TYPE_SIMPLE);
					updateAndResize();
				}
			});

			this.voltageLimitsSelection = new Group(this.cellVoltageMainComposite, SWT.NONE);
			this.voltageLimitsSelection.setBackground(this.surroundingBackground);
			this.voltageLimitsSelection.setText(Messages.getString(MessageIds.OSDE_MSGT0369));
			this.voltageLimitsSelection.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
			RowLayout thisLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.voltageLimitsSelection.setLayout(thisLayout);
			this.voltageLimitsSelection.setBounds(0, 0, 420, 40);
			this.voltageLimitsSelection.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "voltageLimitsSelection.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_9.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.voltageLimitsSelection.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "voltageLimitsSelection.paintControl, event=" + evt); //$NON-NLS-1$
					log.logp(Level.FINE, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, ""+CellVoltageValues.compareVoltageLimits(CellVoltageValues.liPoLimits));
					CellVoltageWindow.this.liPoButton.setSelection(CellVoltageValues.compareVoltageLimits(CellVoltageValues.liPoLimits));
					log.logp(Level.FINE, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, ""+CellVoltageValues.compareVoltageLimits(CellVoltageValues.liIoLimits));
					CellVoltageWindow.this.liIoButton.setSelection(CellVoltageValues.compareVoltageLimits(CellVoltageValues.liIoLimits));
					log.logp(Level.FINE, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, ""+CellVoltageValues.compareVoltageLimits(CellVoltageValues.liFeLimits));
					CellVoltageWindow.this.liFeButton.setSelection(CellVoltageValues.compareVoltageLimits(CellVoltageValues.liFeLimits));
					log.logp(Level.FINE, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, ""+CellVoltageValues.compareVoltageLimits(CellVoltageValues.niMhLimits));
					CellVoltageWindow.this.niMhButton.setSelection(CellVoltageValues.compareVoltageLimits(CellVoltageValues.niMhLimits));
					log.logp(Level.FINE, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, ""+!(CellVoltageWindow.this.liPoButton.getSelection() || CellVoltageWindow.this.liIoButton.getSelection() || CellVoltageWindow.this.liFeButton.getSelection()));
					CellVoltageWindow.this.individualButton.setSelection(!(CellVoltageWindow.this.liPoButton.getSelection() || CellVoltageWindow.this.liIoButton.getSelection() || CellVoltageWindow.this.liFeButton.getSelection() || CellVoltageWindow.this.niMhButton.getSelection()));
				}
			});
			{
				RowData liPoButtonLData = new RowData();
				liPoButtonLData.width = 70;
				liPoButtonLData.height = 20;
				this.liPoButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.liPoButton.setLayoutData(liPoButtonLData);
				this.liPoButton.setBackground(this.surroundingBackground);
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
						CellVoltageWindow.this.niMhButton.setSelection(false);
						CellVoltageWindow.this.individualButton.setSelection(false);
						CellVoltageValues.setVoltageLimits(CellVoltageValues.getLiPoVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						update(true);
					}
				});
			}
			{
				RowData button1LData = new RowData();
				button1LData.width = 70;
				button1LData.height = 18;
				this.liIoButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.liIoButton.setLayoutData(button1LData);
				this.liIoButton.setBackground(this.surroundingBackground);
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
						CellVoltageWindow.this.niMhButton.setSelection(false);
						CellVoltageWindow.this.individualButton.setSelection(false);
						CellVoltageValues.setVoltageLimits(CellVoltageValues.getLiIoVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						update(true);
					}
				});
			}
			{
				RowData button2LData = new RowData();
				button2LData.width = 70;
				button2LData.height = 18;
				this.liFeButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.liFeButton.setLayoutData(button2LData);
				this.liFeButton.setBackground(this.surroundingBackground);
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
						CellVoltageWindow.this.niMhButton.setSelection(false);
						CellVoltageWindow.this.individualButton.setSelection(false);
						CellVoltageValues.setVoltageLimits(CellVoltageValues.getLiFeVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						update(true);
					}
				});
			}
			{
				RowData button2LData = new RowData();
				button2LData.width = 70;
				button2LData.height = 18;
				this.niMhButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.niMhButton.setLayoutData(button2LData);
				this.niMhButton.setBackground(this.surroundingBackground);
				this.niMhButton.setText(Messages.getString(MessageIds.OSDE_MSGT0377));
				this.niMhButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0370));
				this.niMhButton.setSelection(false);
				this.niMhButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.logp(Level.FINEST, CellVoltageWindow.$CLASS_NAME, $METHOD_NAME, "buttonLiPo.widgetSelected, event=" + evt); //$NON-NLS-1$
						CellVoltageWindow.this.liFeButton.setSelection(false);
						CellVoltageWindow.this.liPoButton.setSelection(false);
						CellVoltageWindow.this.liIoButton.setSelection(false);
						CellVoltageWindow.this.niMhButton.setSelection(true);
						CellVoltageWindow.this.individualButton.setSelection(false);
						CellVoltageValues.setVoltageLimits(CellVoltageValues.getNiMhVoltageLimits());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						update(true);
					}
				});
			}
			{
				RowData button1LData1 = new RowData();
				button1LData1.width = 100;
				button1LData1.height = 18;
				this.individualButton = new Button(this.voltageLimitsSelection, SWT.CHECK | SWT.CENTER);
				this.individualButton.setLayoutData(button1LData1);
				this.individualButton.setBackground(this.surroundingBackground);
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
						CellVoltageWindow.this.niMhButton.setSelection(false);
						CellVoltageValues.setVoltageLimits(new CellVoltageValuesDialog(DataExplorer.getInstance(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL).open());
						Channel activeChannel = CellVoltageWindow.this.channels.getActiveChannel();
						RecordSet recordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;
						if (recordSet != null) recordSet.setVoltageLimits();
						update(true);
					}
				});
			}
			this.voltageLimitsSelection.layout();

			this.coverComposite = new Composite(this.cellVoltageMainComposite, SWT.NONE);
			this.coverComposite.setMenu(this.popupmenu);
			FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
			this.coverComposite.setLayout(fillLayout);

			this.cellVoltageMainComposite.setBackground(this.surroundingBackground);
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
				this.voltageValue.setBackground(this.surroundingBackground);
				this.voltageValue.setFont(SWTResourceManager.getFont(this.application, 25, SWT.NORMAL)); //$NON-NLS-1$
				this.voltageValue.setMenu(this.popupmenu);
			}
			{
				this.voltageUnit = new CLabel(this.digitalComposite, SWT.CENTER);
				this.voltageUnit.setText("[V]"); //$NON-NLS-1$
				this.voltageUnit.setBackground(this.surroundingBackground);
				this.voltageUnit.setFont(SWTResourceManager.getFont(this.application, 18, SWT.NORMAL)); //$NON-NLS-1$
				this.voltageUnit.setMenu(this.popupmenu);
			}
			{
				this.capacitiyValue = new CLabel(this.digitalComposite, SWT.CENTER);
				this.capacitiyValue.setText("0000"); //$NON-NLS-1$
				this.capacitiyValue.setBackground(this.surroundingBackground);
				this.capacitiyValue.setFont(SWTResourceManager.getFont(this.application, 25, SWT.NORMAL)); //$NON-NLS-1$
				this.capacitiyValue.setMenu(this.popupmenu);
			}
			{
				this.capacityUnit = new CLabel(this.digitalComposite, SWT.CENTER);
				this.capacityUnit.setText("[mAh]"); //$NON-NLS-1$
				this.capacityUnit.setBackground(this.surroundingBackground);
				this.capacityUnit.setFont(SWTResourceManager.getFont(this.application, 18, SWT.NORMAL)); //$NON-NLS-1$
				this.capacityUnit.setMenu(this.popupmenu);
			}
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
				this.displays.get(i).voltagePaintControl();
				log.log(Level.FINE, "setVoltage cell " + i + " - " + this.voltageVector.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else if (this.coverComposite.isVisible()) {
			updateAndResize();
		}
	}

	/**
	 * method to update cellVoltage window by adding removing cellVoltage displays
	 */
	public void update(boolean forceUpdate) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested() && !this.voltageVector.isEmpty()) {

				updateCellVoltageVector();

				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = forceUpdate || this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName()) || this.oldChannel == null
						|| !this.oldChannel.getName().equals(activeChannel.getName()) || this.displays.size() != this.voltageVector.size();

				log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
				if (isUpdateRequired) {
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
						CellVoltageDisplay display = new CellVoltageDisplay(this.application, this.coverComposite, this.voltageVector.get(i).getVoltage(), this.voltageVector.get(i).getName(), this.voltageVector.get(i).getUnit(), this);
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
			}
			this.cellVoltageMainComposite.layout();
			this.coverComposite.layout();
		}
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
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested() && recordSet.isSyncableDisplayableRecords(false) && this.coverComposite.isVisible()) {
				int cellCount = this.voltageAvg = 0;
				String[] activeRecordKeys = recordSet.getSyncableRecords().toArray(new String[0]);
				for (String recordKey : activeRecordKeys) {
					Record record = recordSet.get(recordKey);
					int index = record.getName().length();
					//log.log(Level.INFO, "record " + record.getName() + " symbol " + record.getSymbol() + " - " + record.getName().substring(index-1, index));
					// algorithm to check if a measurement is a single cell voltage is check match of last character symbol and name U1-Voltage1
					if (record.getSymbol().endsWith(record.getName().substring(index - 1))) { // better use a property to flag as single cell voltage
						if (record.getLast() >= 0) { // last value is current value
							this.voltageVector.add(new CellInfo(record.getLast(), record.getName(), record.getUnit()));
							this.voltageAvg += record.getLast();
							cellCount++;
						}
						//log.log(Level.FINE, "record.getLast() " + record.getLast());
					}
				}
				// add test values here
				//cellCount = addCellVoltages4Test(new int[] {2500, 3500, 3200, 4250}, "ZellenSpannung");
				//cellCount = addCellVoltages4Test(new int[] {2500, 3500, 3200, 4250}, "CellVoltage");
				//cellCount = addCellVoltages4Test(new int[] {4120, 4150, 4175, 4200}, "ZellenSpannung");
				//cellCount = addCellVoltages4Test(new int[] {4120, 4150, 4175, 4200}, "CellVoltage");

				if (cellCount > 0) this.voltageAvg = this.voltageAvg / cellCount;
				//log.log(Level.FINE, "cellCount  = " + cellCount + " cell voltage average = " + this.voltageAvg);
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
		log.log(Level.FINE, "mainSize = " + mainSize.toString());
		if (this.voltageVector.size() > 0) {
			int cellWidth = mainSize.x / 6;
			int x = (6 - CellVoltageWindow.this.voltageVector.size()) * cellWidth / 2;
			int width = mainSize.x - (2 * x);
			Rectangle bounds = new Rectangle(x, 45, width, mainSize.y - 100);
			log.log(Level.FINE, "cover bounds = " + bounds.toString());
			CellVoltageWindow.this.coverComposite.setBounds(bounds);
			CellVoltageWindow.this.digitalComposite.setBounds((mainSize.x - 350) / 2, mainSize.y - 50, 350, 50);

		}
		else {
			CellVoltageWindow.this.coverComposite.setSize(0, 0);
			clearVoltageAndCapacity();
		}
		update(false);
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
		IDevice device = DataExplorer.getInstance().getActiveDevice();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				String[] recordKeys = activeRecordSet.getActiveRecordNames();
				Record record_U = activeRecordSet.getRecord(recordKeys[this.firstMeasurement]); // voltage U
				if (record_U != null) {
					CellVoltageWindow.this.voltageValue.setForeground(record_U.getColor());
					CellVoltageWindow.this.voltageValue.setText(new DecimalFormat("0.00").format(device.translateValue(record_U, (record_U.getLast() / 1000.0))));
					CellVoltageWindow.this.voltageUnit.setText("[" + record_U.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Record record_C = activeRecordSet.getRecord(recordKeys[this.secondMeasurement]); // capacitiy C
				if (record_C != null) {
					CellVoltageWindow.this.capacitiyValue.setForeground(record_C.getColor());
					CellVoltageWindow.this.capacitiyValue.setText(new DecimalFormat("0").format(device.translateValue(record_C, (record_C.getLast() / 1000.0))));
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

	/**
	 * update the voltage limits selection
	 */
	public void updateVoltageLimitsSelection() {
		this.voltageLimitsSelection.redraw();
	}
	
	/**
	 * set the two measurement ordinal to be displayed underneath the cell voltage bars
	 * @param firstMeasurementOrdinal
	 * @param secondMeasurementOrdinal
	 */
	public void setMeasurements(int firstMeasurementOrdinal, int secondMeasurementOrdinal) {
		this.firstMeasurement = firstMeasurementOrdinal;
		this.secondMeasurement = secondMeasurementOrdinal;
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.cellVoltageMainComposite.getClientArea();
		Image tabContentImage = new Image(DataExplorer.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.cellVoltageMainComposite.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}
	
	/**
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color newInnerAreaBackground) {
		this.update(true);
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.surroundingBackground = newSurroundingBackground;
		this.cellVoltageMainComposite.setBackground(newSurroundingBackground);
		this.voltageLimitsSelection.setBackground(newSurroundingBackground);
		this.voltageUnit.setBackground(newSurroundingBackground);
		this.voltageValue.setBackground(newSurroundingBackground);
		this.capacityUnit.setBackground(newSurroundingBackground);
		this.capacitiyValue.setBackground(newSurroundingBackground);
		this.liPoButton.setBackground(newSurroundingBackground);
		this.liIoButton.setBackground(newSurroundingBackground);
		this.liFeButton.setBackground(newSurroundingBackground);
		this.niMhButton.setBackground(newSurroundingBackground);
		this.individualButton.setBackground(newSurroundingBackground);
		this.cellVoltageMainComposite.redraw();
	}
}
