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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.weatronic;

import gde.GDE;
import gde.data.Channels;
import gde.device.IDevice;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.MeasurementControl;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class WeatronicAdapterDialogTabItem extends CTabItem {
	final static Logger							log								= Logger.getLogger(WeatronicAdapterDialogTabItem.class.getName());

	Composite												measurementComposite;
	Button													measurement;
	Label														measurementUnitLabel;
	Label														measurementSymbolLabel;
	Label														tabItemLabel;
	ScrolledComposite								scolledComposite;
	Composite												mainTabComposite;

	final CTabFolder								parent;
	final IDevice										device;																																							// get device specific things, get serial port, ...
	final DataExplorer							application;																																					// interaction with application instance
	final Channels									channels;																																						// interaction with channels, source of all records
	final WeatronicAdapterDialog		dialog;
	final int												channelConfigNumber;
	final List<MeasurementControl>	measurementTypes	= new ArrayList<MeasurementControl>();

	public WeatronicAdapterDialogTabItem(CTabFolder parentTabFolder, WeatronicAdapterDialog parentDialog, int useChannelConfigNumber, IDevice useDevice) {
		super(parentTabFolder, SWT.NONE);
		this.parent = parentTabFolder;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.setText(this.device.getChannelNameReplacement(this.channelConfigNumber));

		create();
	}

	void create() {
		this.scolledComposite = new ScrolledComposite(this.parent, SWT.V_SCROLL);
		this.scolledComposite.setLayout(new FillLayout());
		this.setControl(this.scolledComposite);

		{
			this.mainTabComposite = new Composite(this.scolledComposite, SWT.None);
			GridLayout mainTabCompositeLayout = new GridLayout();
			mainTabCompositeLayout.makeColumnsEqualWidth = true;
			mainTabCompositeLayout.numColumns = 2;
			this.mainTabComposite.setLayout(mainTabCompositeLayout);
			this.mainTabComposite.setSize(610, 350);
			this.scolledComposite.setContent(this.mainTabComposite);
			{
				this.tabItemLabel = new Label(this.mainTabComposite, SWT.CENTER);
				GridData tabItemLabelLData = new GridData();
				tabItemLabelLData.horizontalAlignment = GridData.BEGINNING;
				tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
				tabItemLabelLData.heightHint = 20;
				tabItemLabelLData.widthHint = 300;
				this.tabItemLabel.setLayoutData(tabItemLabelLData);
				this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.BOLD));
				this.tabItemLabel.setText(Messages.getString(MessageIds.GDE_MSGT3709));
			}
			{
				Composite filler = new Composite(this.mainTabComposite, SWT.None);
				GridData fillerLData = new GridData();
				fillerLData.horizontalAlignment = GridData.BEGINNING;
				fillerLData.verticalAlignment = GridData.BEGINNING;
				fillerLData.heightHint = 20;
				fillerLData.widthHint = 300;
				filler.setLayoutData(fillerLData);
			}
			{
				for (int i = 0; i < this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).size(); i++) {
					this.measurementTypes.add(new MeasurementControl(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremtsReplacedNames(this.channelConfigNumber).get(i),
							this.device, 1));
				}
			}
			this.scolledComposite.addControlListener(new ControlListener() {
				@Override
				public void controlResized(ControlEvent evt) {
					WeatronicAdapterDialogTabItem.log.log(java.util.logging.Level.FINEST, "scolledComposite.controlResized, event=" + evt);
					int height = 35 + WeatronicAdapterDialogTabItem.this.device.getChannelMeasuremtsReplacedNames(WeatronicAdapterDialogTabItem.this.parent.getSelectionIndex() + 1).size() * 28 / 2;
					WeatronicAdapterDialogTabItem.this.mainTabComposite.setSize(WeatronicAdapterDialogTabItem.this.scolledComposite.getClientArea().width, height);
				}

				@Override
				public void controlMoved(ControlEvent evt) {
					WeatronicAdapterDialogTabItem.log.log(java.util.logging.Level.FINEST, "scolledComposite.controlMoved, event=" + evt);
					int height = 35 + WeatronicAdapterDialogTabItem.this.device.getChannelMeasuremtsReplacedNames(WeatronicAdapterDialogTabItem.this.parent.getSelectionIndex() + 1).size() * 28 / 2;
					WeatronicAdapterDialogTabItem.this.mainTabComposite.setSize(WeatronicAdapterDialogTabItem.this.scolledComposite.getClientArea().width, height);
				}
			});
		}
	}
}
