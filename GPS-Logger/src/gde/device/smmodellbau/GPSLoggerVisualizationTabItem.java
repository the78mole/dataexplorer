/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import gde.GDE;
import gde.data.Channels;
import gde.device.IDevice;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.messages.Messages;
import gde.ui.MeasurementControl;
import gde.ui.DataExplorer;
import gde.ui.MeasurementControlConfigurable;
import gde.ui.SWTResourceManager;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class GPSLoggerVisualizationTabItem extends CTabItem {
	final static Logger							log									= Logger.getLogger(GPSLoggerVisualizationTabItem.class.getName());

	Composite												measurementComposite;
	Button													measurement;
	Button													inputFileButton;
	Composite												buttonComposite;
	Label														measurementUnitLabel;
	Label														measurementSymbolLabel;
	Label														tabItemLabel;
	Composite												mainTabComposite;

	boolean													isVisibilityChanged	= false;

	final CTabFolder								parent;
	final IDevice										device;																								// get device specific things, get serial port, ...
	final DataExplorer							application;																					// interaction with application instance
	final Channels									channels;																							// interaction with channels, source of all records
	final GPSLoggerDialog						dialog;
	final int												channelConfigNumber;
	final int												measurementCount;
	final int 											measurementOffset;
	final List<Composite>	measurementTypes		= new ArrayList<Composite>();

	public GPSLoggerVisualizationTabItem(CTabFolder parentTabFolder, GPSLoggerDialog parentDialog, int useChannelConfigNumber, IDevice useDevice, String useTabName, int useMeasurementOffset, int useMeasurementCount) {
		super(parentTabFolder, SWT.NONE);
		this.parent = parentTabFolder;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.measurementOffset = useMeasurementOffset;
		this.measurementCount = useMeasurementCount;
		this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.setText(useTabName);

		create();
	}

	void create() {
		this.mainTabComposite = new Composite(this.parent, SWT.NONE);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		this.mainTabComposite.setLayout(mainTabCompositeLayout);
		this.setControl(this.mainTabComposite);
		{
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			for (int i = this.measurementOffset; i < this.measurementOffset+this.measurementCount; i++) {
				if (this.getText().startsWith("M-Link")) {
					this.measurementTypes.add(new MeasurementControlConfigurable(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i), this.device, 1, GDE.STRING_BLANK + (i - this.measurementOffset), "_ML"));
				}
				else if (this.getText().startsWith("UniLog") && i >= this.measurementOffset+this.measurementCount-3) {
					this.measurementTypes.add(new MeasurementControlConfigurable(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i), this.device, 1, "A" + (i - 20),"_UL"));
				}
				else {
					this.measurementTypes.add(new MeasurementControl(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i), this.device, 1));
				}
			}
		}
		{
			this.buttonComposite = new Composite(this.mainTabComposite, SWT.NONE);
			GridData buttonCompositeLData = new GridData();
			buttonCompositeLData.verticalAlignment = GridData.BEGINNING;
			buttonCompositeLData.horizontalAlignment = GridData.CENTER;
			buttonCompositeLData.heightHint = 60;
			this.buttonComposite.setLayoutData(buttonCompositeLData);
			FormLayout buttonCompositeLayout = new FormLayout();
			this.buttonComposite.setLayout(buttonCompositeLayout);
			{
				this.inputFileButton = new Button(this.buttonComposite, SWT.PUSH | SWT.CENTER);
				FormData inputFileButtonLData = new FormData();
				inputFileButtonLData.width = 188;
				inputFileButtonLData.height = 26;
				inputFileButtonLData.left = new FormAttachment(183, 1000, 0);
				inputFileButtonLData.right = new FormAttachment(827, 1000, 0);
				inputFileButtonLData.top = new FormAttachment(150, 1000, 0);
				inputFileButtonLData.bottom = new FormAttachment(1016, 1000, 0);
				buttonCompositeLData.heightHint = 30;
				buttonCompositeLData.widthHint = 292;
				this.inputFileButton.setLayoutData(inputFileButtonLData);
				this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT1702));
				this.inputFileButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (GPSLoggerVisualizationTabItem.this.isVisibilityChanged) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] {GPSLoggerVisualizationTabItem.this.device.getPropertiesFileName()});
							if (GPSLoggerVisualizationTabItem.this.application.openYesNoMessageDialog(GPSLoggerVisualizationTabItem.this.dialog.getDialogShell(), msg) == SWT.YES) {
								log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								GPSLoggerVisualizationTabItem.this.device.storeDeviceProperties();
							}
						}
						GPSLoggerVisualizationTabItem.this.device.openCloseSerialPort();
					}
				});
			}
		}
	}
}
