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
    
    Copyright (c) 2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.data.Channels;
import gde.device.IDevice;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.MeasurementControl;
import gde.ui.MeasurementControlConfigurable;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class GPSLoggerVisualizationControl extends Composite {
	private static final String	MLINK_EXTEND_ML			= "_ML";																														//$NON-NLS-1$
	private static final String	UNILOG_EXTEND_UL		= "_UL";																														//$NON-NLS-1$

	final static Logger					log									= Logger.getLogger(GPSLoggerVisualizationControl.class.getName());

	Composite										measurementComposite;
	Button											measurement;
	Button											inputFileButton;
	Composite										buttonComposite;
	Label												measurementUnitLabel;
	Label												measurementSymbolLabel;
	Label												tabItemLabel;

	boolean											isVisibilityChanged	= false;

	final Widget								parent;
	final IDevice								device;																																								// get device specific things, get serial port, ...
	final DataExplorer					application;																																					// interaction with application instance
	final Channels							channels;																																							// interaction with channels, source of all records
	final GPSLoggerDialog				dialog;
	final int										channelConfigNumber;
	final String								typeName;
	final int										measurementCount;
	final int										measurementOffset;
	final List<Composite>				measurementTypes		= new ArrayList<Composite>();

	public GPSLoggerVisualizationControl(Composite parentComposite, FormData useLayoutData, GPSLoggerDialog parentDialog, int useChannelConfigNumber, IDevice useDevice, String useName,
			int useMeasurementOffset, int useMeasurementCount) {
		super(parentComposite, SWT.NONE);
		this.parent = parentComposite;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.typeName = useName;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.measurementOffset = useMeasurementOffset;
		this.measurementCount = useMeasurementCount;
		this.setLayoutData(useLayoutData);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		this.setLayout(mainTabCompositeLayout);

		create();
	}

	void create() {
		{
			if (this.typeName.equals(Messages.getString(MessageIds.GDE_MSGT2010))) {
				this.tabItemLabel = new Label(this, SWT.CENTER);
				GridData tabItemLabelLData = new GridData();
				tabItemLabelLData.horizontalAlignment = GridData.CENTER;
				tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
				tabItemLabelLData.heightHint = 18;
				tabItemLabelLData.widthHint = 250;
				this.tabItemLabel.setLayoutData(tabItemLabelLData);
				this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
				this.tabItemLabel.setText(this.typeName);
			}
		}
		{
			if (this.channelConfigNumber == 1) {
				//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
				//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
				for (int i = this.measurementOffset; i < this.measurementOffset + this.measurementCount; i++) {
					if (this.typeName.startsWith(Messages.getString(MessageIds.GDE_MSGT2012))) {
						this.measurementTypes.add(new MeasurementControlConfigurable(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i),
								this.device, 1, GDE.STRING_BLANK + (i - this.measurementOffset), GPSLoggerVisualizationControl.MLINK_EXTEND_ML));
					}
					else if (this.typeName.startsWith(Messages.getString(MessageIds.GDE_MSGT2011)) && i >= this.measurementOffset + this.measurementCount - 3) {
						this.measurementTypes.add(new MeasurementControlConfigurable(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i),
								this.device, 1, "A" + (i - 20), GPSLoggerVisualizationControl.UNILOG_EXTEND_UL)); //$NON-NLS-1$
					}
					else {
						this.measurementTypes.add(new MeasurementControl(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i), this.device, 1));
					}
				}
			}
			else if (this.channelConfigNumber == 2) {
				//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
				//Unilog2 15=Voltage, 16=Current, 17=Capacity, 18=Power, 19=Energy, 20=CellBalance, 21=CellVoltage1, 21=CellVoltage2, 23=CellVoltage3, 
				//Unilog2 24=CellVoltage4, 25=CellVoltage5, 26=CellVoltage6, 27=Revolution, 28=ValueA1, 29=ValueA2, 30=ValueA3, 31=InternTemperature
				//M-LINK  32=valAdd00 33=valAdd01 34=valAdd02 35=valAdd03 36=valAdd04 37=valAdd05 38=valAdd06 39=valAdd07 40=valAdd08 41=valAdd09 42=valAdd10 43=valAdd11 44=valAdd12 45=valAdd13 46=valAdd14;
				for (int i = this.measurementOffset; i < this.measurementOffset + this.measurementCount; i++) {
					if (this.typeName.startsWith(Messages.getString(MessageIds.GDE_MSGT2012))) {
						this.measurementTypes.add(new MeasurementControlConfigurable(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i),
								this.device, 1, GDE.STRING_BLANK + (i - this.measurementOffset), GPSLoggerVisualizationControl.MLINK_EXTEND_ML));
					}
					else if (this.typeName.startsWith(Messages.getString(MessageIds.GDE_MSGT2011)) && i > 27 && i < 31) {
						this.measurementTypes.add(new MeasurementControlConfigurable(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i),
								this.device, 1, "A" + (i - 27), GPSLoggerVisualizationControl.UNILOG_EXTEND_UL)); //$NON-NLS-1$
					}
					else {
						this.measurementTypes.add(new MeasurementControl(this, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i), this.device, 1));
					}
				}
			}
		}
	}
}
