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
package osde.ui.dialog.edit;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.ChannelType;
import osde.device.ChannelTypes;
import osde.device.DeviceConfiguration;
import osde.device.MeasurementType;
import osde.device.ObjectFactory;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.SWTResourceManager;

/**
 * class defining a CTabItem with ChannelType configuration data
 * @author Winfried Brügmann
 */
public class ChannelTypeTabItem extends CTabItem {
	final static Logger	log								= Logger.getLogger(ChannelTypeTabItem.class.getName());

	Composite						channelConfigComposite;
	Button							channelConfigAddButton;
	Label								channelConfigLabel;
	CCombo							channelConfigTypeCombo;
	Text								channelConfigText;
	CTabFolder					measurementsTabFolder;

	ChannelTypes				channelConfigType	= ChannelTypes.TYPE_OUTLET;
	String							channelConfigName	= Messages.getString(MessageIds.OSDE_MSGT0525);

	final CTabFolder		channelConfigInnerTabFolder;
	final String				tabName;
	DeviceConfiguration	deviceConfig;
	int									channelConfigNumber;
	ChannelType					channelType;

	public ChannelTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style);
		this.channelConfigInnerTabFolder = parent;
		this.tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		ChannelTypeTabItem.log.log(Level.FINE, "ChannelTypeTabItem " + this.tabName); //$NON-NLS-1$
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 * @param useChannelType the ChannelType to set
	 */
	public void setChannelType(DeviceConfiguration useDeviceConfig, ChannelType useChannelType, int useChannelConfigNumber) {
		ChannelTypeTabItem.log.log(Level.FINE, "ChannelTypeTabItem.setChannelType"); //$NON-NLS-1$
		this.deviceConfig = useDeviceConfig;
		this.channelType = useChannelType;
		this.channelConfigNumber = useChannelConfigNumber;
		this.channelConfigComposite.redraw();
		this.setText(this.tabName + this.channelType.getName());

		//MeasurementType begin
		int measurementTypeCount = this.channelType.getMeasurement().size();
		int actualTabItemCount = this.measurementsTabFolder.getItemCount();
		if (measurementTypeCount < actualTabItemCount) {
			for (int i = measurementTypeCount; i < actualTabItemCount; i++) {
				MeasurementTypeTabItem tmpMeasurementTypeTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(measurementTypeCount);
				if (tmpMeasurementTypeTabItem.measurementPropertiesTabFolder != null) { // dispose PropertyTypes
					for (CTabItem tmpPropertyTypeTabItem : tmpMeasurementTypeTabItem.measurementPropertiesTabFolder.getItems()) {
						((PropertyTypeTabItem) tmpPropertyTypeTabItem).dispose();
					}
					tmpMeasurementTypeTabItem.measurementPropertiesTabItem.dispose();
				}
				if (tmpMeasurementTypeTabItem.statisticsTypeTabItem != null) { // dispose StatisticsType
					tmpMeasurementTypeTabItem.statisticsTypeTabItem.dispose();
				}
				tmpMeasurementTypeTabItem.dispose();
			}
		}
		else if (measurementTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < measurementTypeCount; i++) {
				new MeasurementTypeTabItem(this.measurementsTabFolder, SWT.NONE, i);
			}
		}
		this.measurementsTabFolder.getItem(measurementTypeCount-1).setShowClose(true);
		for (int i = 0; i < measurementTypeCount; i++) {
			MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(i);
			measurementTabItem.setMeasurementType(this.deviceConfig, this.channelType.getMeasurement().get(i), this.channelConfigNumber);
		}
		//MeasurementType end
		
		this.channelConfigInnerTabFolder.setSelection(0);
	}

	public ChannelTypeTabItem(CTabFolder parent, int style, int index, ChannelType useChannelType) {
		super(parent, style);
		this.channelConfigInnerTabFolder = parent;
		this.tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		this.channelType = useChannelType;
		initGUI();
	}
	
	public synchronized ChannelTypeTabItem clone() {
		return new ChannelTypeTabItem(this);
	}
	
	/**
	 * copy constructor
	 * @param copyFrom
	 */
	private ChannelTypeTabItem(ChannelTypeTabItem copyFrom) {
		super(copyFrom.channelConfigInnerTabFolder, SWT.CLOSE);
		this.channelConfigInnerTabFolder = copyFrom.channelConfigInnerTabFolder;
		this.deviceConfig = copyFrom.deviceConfig;
		this.channelConfigNumber = this.channelConfigInnerTabFolder.getItemCount();
		this.channelType = new ObjectFactory().createChannelType();
		this.channelConfigType	= copyFrom.channelConfigType;
		this.channelConfigName	= copyFrom.channelConfigName;
		this.channelType.setName(this.channelConfigType == ChannelTypes.TYPE_OUTLET ? this.channelConfigName : "newConguration");
		this.channelType.setType(this.channelConfigType);
		this.tabName = OSDE.STRING_BLANK + this.channelConfigNumber + OSDE.STRING_BLANK + (this.deviceConfig != null ? this.channelType.getName() : OSDE.STRING_EMPTY) ;
		initGUI();
		
		//MeasurementType begin - fix number tab items
		int measurementTypeCount =copyFrom.channelType.getMeasurement().size();
		int actualTabItemCount = this.measurementsTabFolder.getItemCount();
		if (measurementTypeCount < actualTabItemCount) {
			for (int i = measurementTypeCount; i < actualTabItemCount; i++) {
				MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(measurementTypeCount);
				measurementTabItem.dispose();
			}
		}
		else if (measurementTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < measurementTypeCount; i++) {
				new MeasurementTypeTabItem(this.measurementsTabFolder, SWT.NONE, i);
			}
		}
		this.measurementsTabFolder.getItem(measurementTypeCount-1).setShowClose(true);
		//MeasurementType end
		if (this.deviceConfig != null) {
			this.deviceConfig.addChannelType(this.channelType);
			//copy existing measurements
			for (int i = 0; i < copyFrom.channelType.getMeasurement().size(); ++i) {
				MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem) this.measurementsTabFolder.getItem(i);
				MeasurementType tmpMeasurementType = copyFrom.channelType.getMeasurement().get(i).clone();
				measurementTabItem.setMeasurementType(this.deviceConfig, tmpMeasurementType, this.channelConfigNumber);
				this.channelType.getMeasurement().add(tmpMeasurementType);
			}	
		}			
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			{
				this.channelConfigComposite = new Composite(this.channelConfigInnerTabFolder, SWT.NONE);
				this.setControl(this.channelConfigComposite);
				this.channelConfigComposite.setLayout(null);
				this.channelConfigComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						ChannelTypeTabItem.log.log(Level.FINEST, "channelConfigComposite.paintControl, event=" + evt); //$NON-NLS-1$
						if (ChannelTypeTabItem.this.channelConfigComposite.isVisible()) {
							if (ChannelTypeTabItem.this.channelType != null) {
								ChannelTypeTabItem.this.channelConfigType = ChannelTypeTabItem.this.channelType.getType();
								ChannelTypeTabItem.this.channelConfigTypeCombo.select(ChannelTypeTabItem.this.channelConfigType.ordinal());
								ChannelTypeTabItem.this.channelConfigName = ChannelTypeTabItem.this.channelType.getName();
								ChannelTypeTabItem.this.channelConfigText.setText(ChannelTypeTabItem.this.channelConfigName);
							}
						}
					}
				});
				{
					this.channelConfigTypeCombo = new CCombo(this.channelConfigComposite, SWT.BORDER);
					this.channelConfigTypeCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.channelConfigTypeCombo.setBounds(6, 9, 121, 20);
					this.channelConfigTypeCombo.setItems(new String[] { "TYPE_OUTLET", "TYPE_CONFIG" }); //$NON-NLS-1$ //$NON-NLS-2$
					this.channelConfigTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							ChannelTypeTabItem.log.log(Level.FINEST, "channelConfigTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							ChannelTypeTabItem.this.channelConfigType = ChannelTypes.valueOf(ChannelTypeTabItem.this.channelConfigTypeCombo.getText());
							if (ChannelTypeTabItem.this.channelType != null) {
								ChannelTypeTabItem.this.channelType.setType(ChannelTypeTabItem.this.channelConfigType);
								ChannelTypeTabItem.this.deviceConfig.setChangePropery(true);
							}
						}
					});
				}
				{
					this.channelConfigText = new Text(this.channelConfigComposite, SWT.BORDER);
					this.channelConfigText.setText(Messages.getString(MessageIds.OSDE_MSGT0525));
					this.channelConfigText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.channelConfigText.setBounds(147, 9, 128, 20);
					this.channelConfigText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							ChannelTypeTabItem.log.log(Level.FINEST, "channelConfigText.keyReleased, event=" + evt); //$NON-NLS-1$
							ChannelTypeTabItem.this.channelConfigName = ChannelTypeTabItem.this.channelConfigText.getText().trim();
							if (ChannelTypeTabItem.this.channelType != null) {
								ChannelTypeTabItem.this.channelType.setName(ChannelTypeTabItem.this.channelConfigName);
								ChannelTypeTabItem.this.deviceConfig.setChangePropery(true);
								ChannelTypeTabItem.this.setText(ChannelTypeTabItem.this.tabName + ChannelTypeTabItem.this.channelConfigName);
							}
						}
					});
				}
				{
					this.channelConfigLabel = new Label(this.channelConfigComposite, SWT.CENTER);
					this.channelConfigLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0526));
					this.channelConfigLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.channelConfigLabel.setBounds(289, 9, 279, 20);
				}
				{
					this.measurementsTabFolder = new CTabFolder(this.channelConfigComposite, SWT.NONE | SWT.BORDER);
					this.measurementsTabFolder.setBounds(0, 35, 622, 225);
					{
						//create initial measurement type
						new MeasurementTypeTabItem(this.measurementsTabFolder, SWT.NONE, 0);
					}
					this.measurementsTabFolder.setSelection(0);
					this.measurementsTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
						@Override
						public void restore(CTabFolderEvent evt) {
							ChannelTypeTabItem.log.log(Level.FINE, "measurementsTabFolder.restore, event=" + evt); //$NON-NLS-1$
							((CTabItem) evt.item).getControl();
						}

						@Override
						public void close(CTabFolderEvent evt) {
							ChannelTypeTabItem.log.log(Level.FINE, "measurementsTabFolder.close, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem tabItem = ((MeasurementTypeTabItem) evt.item);
							if (deviceConfig != null) {
								deviceConfig.removeMeasurementFromChannel(ChannelTypeTabItem.this.channelConfigNumber, tabItem.measurementType);
							}
							ChannelTypeTabItem.this.measurementsTabFolder.setSelection(measurementsTabFolder.getSelectionIndex() - 1);
							tabItem.dispose();
							
							int itemCount = measurementsTabFolder.getItemCount();
							if (itemCount > 1)
								ChannelTypeTabItem.this.measurementsTabFolder.getItem(itemCount-1).setShowClose(true);
						}
					});
				}
				{
					this.channelConfigAddButton = new Button(this.channelConfigComposite, SWT.PUSH | SWT.CENTER);
					this.channelConfigAddButton.setText("+"); //$NON-NLS-1$
					this.channelConfigAddButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.channelConfigAddButton.setBounds(574, 9, 42, 19);
					this.channelConfigAddButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0524));
					this.channelConfigAddButton.setSize(40, 20);
					this.channelConfigAddButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							ChannelTypeTabItem.log.log(Level.FINEST, "channelConfigAddButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							ChannelTypeTabItem.this.channelConfigInnerTabFolder.getItem(ChannelTypeTabItem.this.channelConfigInnerTabFolder.getItemCount()-1).setShowClose(false);
							ChannelTypeTabItem.this.clone();
						}
					});
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
