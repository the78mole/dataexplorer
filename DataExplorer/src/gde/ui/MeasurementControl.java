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
package osde.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import osde.OSDE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.device.IDevice;
import osde.device.MeasurementType;

/**
 * This class enable control of the visualization control of record and the corresponding measurement type active status
 * @author Winfried Brügmann
 */
public class MeasurementControl extends Composite {
	final static Logger						log											= Logger.getLogger(MeasurementControl.class.getName());

	Composite											measurementComposite;
	Button												measurement;
	Button												inputFileButton;
	Composite											buttonComposite;
	Label													measurementUnitLabel;
	Label													measurementSymbolLabel;

	final IDevice									device;																																						// get device specific things, get serial port, ...
	final OpenSerialDataExplorer	application;																																			// interaction with application instance
	final Channels								channels;																																					// interaction with channels, source of all records
	final DeviceDialog						dialog;
	final	MeasurementType					measurementType;
	final int 										ordinal;


	public MeasurementControl(Composite parentComposite, DeviceDialog parentDialog, int useOrdinal, MeasurementType useMeasurementType, IDevice useDevice) {
		super(parentComposite, SWT.NONE);
		this.dialog = parentDialog;
		this.ordinal = useOrdinal;
		this.measurementType = useMeasurementType;
		this.device = useDevice;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();

		RowLayout thisLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		GridData thisLData = new GridData();
		thisLData.horizontalAlignment = GridData.BEGINNING;
		thisLData.verticalAlignment = GridData.BEGINNING;
		thisLData.heightHint = 25;
		this.setLayoutData(thisLData);
		this.setLayout(thisLayout);
		{
			measurement = new Button(this, SWT.CHECK | SWT.LEFT);
			RowData measurementLData = new RowData();
			measurementLData.width = 180;
			measurementLData.height = 20;
			measurement.setLayoutData(measurementLData);
			measurement.setText(measurementType.getName());
			measurement.setSelection(measurementType.isActive());
			measurement.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "measurement.widgetSelected, event=" + evt);
					boolean isVisible = measurement.getSelection();
					Channel activeChannel = channels.getActiveChannel();
					if (activeChannel != null) {
						device.setMeasurementActive(activeChannel.getNumber(), ordinal, isVisible);
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
							activeRecordSet.get(activeRecordSet.getRecordNames()[ordinal]).setActive(isVisible);
							activeRecordSet.get(activeRecordSet.getRecordNames()[ordinal]).setVisible(isVisible);
							activeRecordSet.get(activeRecordSet.getRecordNames()[ordinal]).setDisplayable(isVisible);
							device.updateVisibilityStatus(activeRecordSet);
							application.updateGraphicsWindow();
						}
					}
					dialog.enableSaveButton(true);
				}
			});
		}
		{
			measurementSymbolLabel = new Label(this, SWT.CENTER);
			RowData measurementSymbolLabelLData = new RowData();
			measurementSymbolLabelLData.width = 50;
			measurementSymbolLabelLData.height = 20;
			measurementSymbolLabel.setLayoutData(measurementSymbolLabelLData);
			measurementSymbolLabel.setText(measurementType.getSymbol());
		}
		{
			measurementUnitLabel = new Label(this, SWT.CENTER);
			RowData measurementUnitLabelLData = new RowData();
			measurementUnitLabelLData.width = 50;
			measurementUnitLabelLData.height = 20;
			measurementUnitLabel.setLayoutData(measurementUnitLabelLData);
			measurementUnitLabel.setText(OSDE.STRING_LEFT_BRACKET + measurementType.getUnit() + OSDE.STRING_RIGHT_BRACKET);
		}

		}

}
