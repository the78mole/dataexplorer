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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.ui;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * This class enable control of the visualization control of record and the corresponding measurement type active status
 * @author Winfried Brügmann
 */
public class MeasurementControlConfigurable extends Composite {
	final static Logger		log	= Logger.getLogger(MeasurementControlConfigurable.class.getName());

	Composite							measurementComposite;
	Button								measurement;
	Text									measurementName;
	CLabel								measurementUnit;
	Text									measurementSymbol;
	Button								measurementSynch;
	Button								inputFileButton;
	Composite							buttonComposite;

	final IDevice					device;																																// get device specific things, get serial port, ...
	final DataExplorer		application;																														// interaction with application instance
	final Channels				channels;																															// interaction with channels, source of all records
	final DeviceDialog		dialog;
	final MeasurementType	measurementType;
	final int							ordinal;
	final int							channelConfigNumber;
	final String					address;
	final String					filterExtend;

	/**
	 * create a check button to activate measurement while displaying name, symbol and unit
	 * @param parentComposite
	 * @param parentDialog
	 * @param useChannelConfigNumber
	 * @param useOrdinal
	 * @param useMeasurementType
	 * @param useDevice
	 * @param horizontalSpan
	 * @param useAddress
	 * @param useFilterExtend
	 */
	public MeasurementControlConfigurable(Composite parentComposite, DeviceDialog parentDialog, int useChannelConfigNumber, int useOrdinal, MeasurementType useMeasurementType, IDevice useDevice,
			int horizontalSpan, String useAddress, String useFilterExtend) {
		super(parentComposite, SWT.NONE);
		this.dialog = parentDialog;
		this.channelConfigNumber = useChannelConfigNumber;
		this.ordinal = useOrdinal;
		this.measurementType = useMeasurementType;
		this.device = useDevice;
		this.address = useAddress;
		this.filterExtend = useFilterExtend;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();

		RowLayout thisLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		GridData thisLData = new GridData();
		thisLData.horizontalSpan = horizontalSpan;
		thisLData.horizontalAlignment = GridData.BEGINNING;
		thisLData.verticalAlignment = GridData.BEGINNING;
		thisLData.heightHint = 23;
		this.setLayoutData(thisLData);
		this.setLayout(thisLayout);
		{
			this.measurement = new Button(this, SWT.CHECK | SWT.RIGHT);
			RowData measurementLData = new RowData();
			measurementLData.width = 50;
			measurementLData.height = 20;
			this.measurement.setLayoutData(measurementLData);
			this.measurement.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.measurement.setText(String.format(" %2s", this.address)); //$NON-NLS-1$
			this.measurement.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0299));
			this.measurement.setSelection(this.measurementType.isActive());
			this.measurement.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "measurement.widgetSelected, event=" + evt); //$NON-NLS-1$
					boolean isVisible = MeasurementControlConfigurable.this.measurement.getSelection();
					MeasurementControlConfigurable.this.device.setMeasurementActive(MeasurementControlConfigurable.this.channelConfigNumber, MeasurementControlConfigurable.this.ordinal, isVisible);
					Channel activeChannel = MeasurementControlConfigurable.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
							activeRecordSet.get(activeRecordSet.getRecordNames()[MeasurementControlConfigurable.this.ordinal]).setActive(isVisible);
							activeRecordSet.get(activeRecordSet.getRecordNames()[MeasurementControlConfigurable.this.ordinal]).setVisible(isVisible);
							activeRecordSet.get(activeRecordSet.getRecordNames()[MeasurementControlConfigurable.this.ordinal]).setDisplayable(isVisible);
							MeasurementControlConfigurable.this.device.updateVisibilityStatus(activeRecordSet, false);
							MeasurementControlConfigurable.this.application.updateGraphicsWindow();
						}
					}
					MeasurementControlConfigurable.this.dialog.enableSaveButton(true);
				}
			});
		}
		{
			this.measurementName = new Text(this, SWT.CENTER | SWT.BORDER);
			RowData measurementNameLData = new RowData();
			measurementNameLData.width = 120;
			measurementNameLData.height = 12;
			this.measurementName.setLayoutData(measurementNameLData);
			this.measurementName.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.measurementName.setText(this.measurementType.getName().endsWith(this.filterExtend) ? this.measurementType.getName().substring(0, this.measurementType.getName().length() - 3)
					: this.measurementType.getName());
			this.measurementName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent evt) {
					updateName(MeasurementControlConfigurable.this.measurementName.getText());
					MeasurementControlConfigurable.this.dialog.enableSaveButton(true);
				}
			});
		}
		{
			this.measurementSymbol = new Text(this, SWT.CENTER | SWT.BORDER);
			RowData measurementSymbolLabelLData = new RowData();
			measurementSymbolLabelLData.width = 35;
			measurementSymbolLabelLData.height = 12;
			this.measurementSymbol.setLayoutData(measurementSymbolLabelLData);
			this.measurementSymbol.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE - 1, SWT.NORMAL));
			this.measurementSymbol.setText(this.measurementType.getSymbol());
			this.measurementSymbol.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent evt) {
					MeasurementControlConfigurable.this.measurementType.setSymbol(MeasurementControlConfigurable.this.measurementSymbol.getText());
					MeasurementControlConfigurable.this.dialog.enableSaveButton(true);
				}
			});
		}
		{
			this.measurementUnit = new CLabel(this, SWT.CENTER);
			RowData measurementUnitLabelLData = new RowData();
			measurementUnitLabelLData.width = 35;
			measurementUnitLabelLData.height = 20;
			this.measurementUnit.setLayoutData(measurementUnitLabelLData);
			this.measurementUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE - 1, SWT.NORMAL));
			this.measurementUnit.setText(GDE.STRING_LEFT_BRACKET + this.measurementType.getUnit() + GDE.STRING_RIGHT_BRACKET);
		}
		{
			Composite spacer = new Composite(this, SWT.NONE);
			RowData measurementLData = new RowData();
			measurementLData.width = 3;
			measurementLData.height = 20;
			spacer.setLayoutData(measurementLData);
		}
		{
			this.measurementSynch = new Button(this, SWT.CHECK);
			RowData measurementLData = new RowData();
			measurementLData.width = 50;
			measurementLData.height = 20;
			this.measurementSynch.setLayoutData(measurementLData);
			this.measurementSynch.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.measurementSynch.setText(String.format(" %2s", this.measurementType.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()) != null ? this.measurementType.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()).getValue() : GDE.STRING_EMPTY)); //$NON-NLS-1$
			this.measurementSynch.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0298));
			this.measurementSynch.setSelection(this.measurementType.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()) != null ? true : false);
			this.measurementSynch.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "measurementSynch.widgetSelected, event=" + evt); //$NON-NLS-1$
					MeasurementControlConfigurable.this.measurementSynch.setSelection(synchronizeRecord(MeasurementControlConfigurable.this.measurementSynch.getSelection()));
					MeasurementControlConfigurable.this.dialog.enableSaveButton(true);
				}
			});
		}
	}

	/**
	 * set anew name to a measurement and active record
	 * @param newName
	 */
	void updateName(String newName) {
		for (int i = this.filterExtend.length(); i > 0; i--) {
			if (newName.endsWith(this.filterExtend.substring(0, i))) {
				newName = newName.substring(0, newName.length() - this.filterExtend.length());
				break;
			}
		}
		this.measurementType.setName(newName + this.filterExtend);
		RecordSet activeRecordSet = this.application.getActiveRecordSet();
		if (activeRecordSet != null) {
			activeRecordSet.get(this.ordinal).setName(newName + this.filterExtend);
		}
	}

	/**
	 * synchronize scale to other record
	 */
	boolean synchronizeRecord(boolean enable) {
		boolean isEnabled = !enable;
		if (enable) {
			RecordSet activeRecordSet = this.application.getActiveRecordSet();
			if (activeRecordSet != null) {
				String syncMeasurementName = this.measurementType.getName().endsWith(this.filterExtend) ? this.measurementType.getName().substring(0,	this.measurementType.getName().length() - this.filterExtend.length()) : this.measurementType.getName();
				if (activeRecordSet.get(syncMeasurementName) != null) {
					this.device.setMeasurementPropertyValue(this.channelConfigNumber, this.ordinal, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value(), DataTypes.INTEGER,	activeRecordSet.get(syncMeasurementName).getOrdinal());
					isEnabled = true;
				}
			}
		}
		else {
			this.device.setMeasurementPropertyValue(this.channelConfigNumber, this.ordinal, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value(), DataTypes.INTEGER, null);
			isEnabled = false;
		}
		this.measurementSynch.setText(String.format(" %2s", this.measurementType.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()) != null ? this.measurementType.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()).getValue() : GDE.STRING_EMPTY)); //$NON-NLS-1$
		return isEnabled;
	}
}
