/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.tttronix;

import gde.GDE;
import gde.data.Channels;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class QcCopterTabItem extends CTabItem {
	final static Logger							log									= Logger.getLogger(QcCopterTabItem.class.getName());

	Composite												measurementComposite;
	Button													measurement;
	Button													inputFileButton;
	Composite												buttonComposite;
	Button													stopCollectDataButton;
	Button													startCollectDataButton;
	Label														measurementUnitLabel;
	Label														measurementSymbolLabel;
	Label														tabItemLabel;
	Composite												mainTabComposite;

	boolean													isVisibilityChanged	= false;

	final CTabFolder								parent;
	final QcCopter									device;																																								// get device specific things, get serial port, ...
	final QcCopterSerialPort				serialPort;																																						// get device serial port, ...
	final DataExplorer							application;																																					// interaction with application instance
	final Channels									channels;																																							// interaction with channels, source of all records
	final QcCopterDialog						dialog;
	final int												channelConfigNumber;
	final List<MeasurementControl>	measurementTypes		= new ArrayList<MeasurementControl>();

	public QcCopterTabItem(CTabFolder parentTabFolder, QcCopterDialog parentDialog, int useChannelConfigNumber, QcCopter useDevice) {
		super(parentTabFolder, SWT.NONE);
		this.parent = parentTabFolder;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.serialPort = useDevice.getSerialPort();
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.setText(this.device.getChannelName(this.channelConfigNumber));

		create();
	}

	void create() {
		this.mainTabComposite = new Composite(this.parent, SWT.NONE);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		mainTabCompositeLayout.numColumns = 2;
		this.mainTabComposite.setLayout(mainTabCompositeLayout);
		this.setControl(this.mainTabComposite);
		{
			this.tabItemLabel = new Label(this.mainTabComposite, SWT.CENTER);
			GridData tabItemLabelLData = new GridData();
			tabItemLabelLData.horizontalSpan = 2;
			tabItemLabelLData.grabExcessHorizontalSpace = true;
			tabItemLabelLData.horizontalAlignment = GridData.CENTER;
			tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
			tabItemLabelLData.heightHint = 25;
			tabItemLabelLData.widthHint = 595;
			this.tabItemLabel.setLayoutData(tabItemLabelLData);
			this.tabItemLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE+2, SWT.BOLD));
			this.tabItemLabel.setText(Messages.getString(MessageIds.GDE_MSGT1901));
		}
		{
			//measurements
			for (int i = 0; i < this.device.getChannelMeasuremts(this.channelConfigNumber).size(); i++) {
				this.measurementTypes.add(new MeasurementControl(this.mainTabComposite, this.dialog, this.channelConfigNumber, i, this.device.getChannelMeasuremts(this.channelConfigNumber).get(i), this.device, i==10?2:1));
			}
		}
//		{
//			this.buttonComposite = new Composite(this.mainTabComposite, SWT.NONE);
//			GridData buttonCompositeLData = new GridData();
//			buttonCompositeLData.horizontalSpan = 2;
//			buttonCompositeLData.grabExcessHorizontalSpace = true;
//			buttonCompositeLData.verticalAlignment = GridData.BEGINNING;
//			buttonCompositeLData.horizontalAlignment = GridData.CENTER;
//			buttonCompositeLData.heightHint = 35;
//			buttonCompositeLData.widthHint = 540;
//			this.buttonComposite.setLayoutData(buttonCompositeLData);
//			FormLayout buttonCompositeLayout = new FormLayout();
//			this.buttonComposite.setLayout(buttonCompositeLayout);
//			{
//				FormData startCollectDataButtonLData = new FormData();
//				startCollectDataButtonLData.width = 120;
//				startCollectDataButtonLData.height = 30;
//				startCollectDataButtonLData.bottom = new FormAttachment(1000, 1000, 0);
//				startCollectDataButtonLData.left = new FormAttachment(0, 1000, 25);
//				this.startCollectDataButton = new Button(this.buttonComposite, SWT.PUSH | SWT.CENTER);
//				this.startCollectDataButton.setLayoutData(startCollectDataButtonLData);
//				this.startCollectDataButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0274));
//				this.startCollectDataButton.setToolTipText("hiermit kann das terminal abgeschaltet");
//				this.startCollectDataButton.addSelectionListener(new SelectionAdapter() {
//					@Override
//					public void widgetSelected(SelectionEvent evt) {
//						QcCopterTabItem.log.log(Level.FINEST, "startCollectDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
//						if (!QcCopterTabItem.this.serialPort.isConnected()) {
//							try {
//								Channel activChannel = Channels.getInstance().getActiveChannel();
//								if (activChannel != null) {
//									QcCopterTabItem.this.dialog.dataGatherThread = new GathererThread(QcCopterTabItem.this.application, QcCopterTabItem.this.device, QcCopterTabItem.this.serialPort, activChannel.getNumber(), QcCopterTabItem.this.dialog);
//									try {
//										QcCopterTabItem.this.dialog.dataGatherThread.start();
//									}
//									catch (RuntimeException e) {
//										log.log(Level.WARNING, e.getMessage(), e);
//									}
//									QcCopterTabItem.this.buttonComposite.redraw();
//								}
//							}
//							catch (Exception e) {
//								if (QcCopterTabItem.this.dialog.dataGatherThread != null && QcCopterTabItem.this.dialog.dataGatherThread.isCollectDataStopped) {
//									QcCopterTabItem.this.dialog.dataGatherThread.stopDataGatheringThread(false, e);
//								}
//								QcCopterTabItem.this.buttonComposite.redraw();
//								QcCopterTabItem.this.application.updateGraphicsWindow();
//								QcCopterTabItem.this.application.openMessageDialog(QcCopterTabItem.this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0023, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
//							}
//						}
//					}
//				});
//				//this.startCollectDataButton.addMouseTrackListener(this.dialog.mouseTrackerEnterFadeOut);
//			}
//			{
//				FormData stopColletDataButtonLData = new FormData();
//				stopColletDataButtonLData.width = 120;
//				stopColletDataButtonLData.height = 30;
//				stopColletDataButtonLData.bottom = new FormAttachment(1000, 1000, 0);
//				stopColletDataButtonLData.right = new FormAttachment(1000, 1000, -33);
//				this.stopCollectDataButton = new Button(this.buttonComposite, SWT.PUSH | SWT.CENTER);
//				this.stopCollectDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
//				this.stopCollectDataButton.setLayoutData(stopColletDataButtonLData);
//				this.stopCollectDataButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0275));
//				this.stopCollectDataButton.setEnabled(false);
//				this.stopCollectDataButton.addSelectionListener(new SelectionAdapter() {
//					@Override
//					public void widgetSelected(SelectionEvent evt) {
//						QcCopterTabItem.log.log(Level.FINEST, "stopColletDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
//						if (QcCopterTabItem.this.dialog.dataGatherThread != null && QcCopterTabItem.this.serialPort.isConnected()) {
//							QcCopterTabItem.this.dialog.dataGatherThread.stopDataGatheringThread(false, null);
//						}
//						QcCopterTabItem.this.buttonComposite.redraw();
//					}
//				});
//				//this.stopCollectDataButton.addMouseTrackListener(this.dialog.mouseTrackerEnterFadeOut);
//			}
//		}
	}
}
