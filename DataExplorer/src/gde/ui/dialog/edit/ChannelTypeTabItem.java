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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
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
import osde.device.ChannelTypes;
import osde.device.DeviceConfiguration;

/**
 * class defining a CTabItem with ChannelType configuration data
 * @author Winfried Brügmann
 */
public class ChannelTypeTabItem extends CTabItem {
	final static Logger						log			= Logger.getLogger(ChannelTypeTabItem.class.getName());

	Composite channelConfigComposite;
	Button addMeasurementButton;
	Composite statisticsComposite;
	Composite MeasurementPropertiesComposite;
	Label label1;
	CTabItem measurementPropertyTabItem;
	CTabFolder measurementsPropertiesTabFolder;
	CTabItem measurementStatisticsTabItem;
	CTabItem measurementPropertiesTabItem;
	CTabFolder channelConfigMeasurementPropertiesTabFolder;
	Button channelConfigAddButton;
	Label channelConfigLabel;
	CCombo channelConfigTypeCombo;
	Text channelConfigText;
	
	ChannelTypes channelConfigType = ChannelTypes.TYPE_OUTLET;
	String channelConfigName = "Outlet";

	final CTabFolder channelConfigInnerTabFolder;
	final String tabName;
	DeviceConfiguration deviceConfig;
	
	
	public ChannelTypeTabItem(CTabFolder parent, int index) {
		super(parent, SWT.NONE, index);
		channelConfigInnerTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 */
	public void setDeviceConfig(DeviceConfiguration useDeviceConfig) {
		this.deviceConfig = useDeviceConfig;
	}
	
	public ChannelTypeTabItem(CTabFolder parent, int index, DeviceConfiguration useDeviceConfig) {
		super(parent, SWT.NONE, index);
		channelConfigInnerTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		deviceConfig = useDeviceConfig;
		initGUI();
	}

	private void initGUI() {
		try {
			this.setText(tabName);
			{
				channelConfigComposite = new Composite(channelConfigInnerTabFolder, SWT.NONE);
				this.setControl(channelConfigComposite);
				channelConfigComposite.setLayout(null);
				channelConfigComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						System.out.println("channelConfigComposite.paintControl, event="+evt);
						if (deviceConfig != null) {
							int index = channelConfigInnerTabFolder.getSelectionIndex() + 1;
							channelConfigType = ChannelTypes.values()[deviceConfig.getChannelType(index)];
							channelConfigTypeCombo.select(channelConfigType.ordinal());
							channelConfigName = deviceConfig.getChannelName(index);
							channelConfigText.setText(channelConfigName);
						}
					}
				});
				{
					channelConfigTypeCombo = new CCombo(channelConfigComposite, SWT.BORDER);
					channelConfigTypeCombo.setBounds(6, 9, 121, 19);
					channelConfigTypeCombo.setItems(new String[] {"TYPE_OUTLET", "TYPE_CONFIG"});
					channelConfigTypeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("channelConfigTypeCombo.widgetSelected, event="+evt);
							//TODO add your code for channelConfigTypeCombo.widgetSelected
						}
					});
				}
				{
					channelConfigText = new Text(channelConfigComposite, SWT.BORDER);
					channelConfigText.setText("Outlet");
					channelConfigText.setBounds(147, 9, 128, 19);
					channelConfigText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							System.out.println("channelConfigText.keyReleased, event="+evt);
							//TODO add your code for channelConfigText.keyReleased
						}
					});
				}
				{
					channelConfigLabel = new Label(channelConfigComposite, SWT.CENTER);
					channelConfigLabel.setText("complete definitions before adding new");
					channelConfigLabel.setBounds(289, 9, 279, 19);
				}
//				{
//					measurementsTabFolder = new CTabFolder(channelConfigComposite, SWT.CLOSE | SWT.BORDER);
//					measurementsTabFolder.setBounds(0, 34, 622, 225);
//					{
//						measurementTabItem = new CTabItem(measurementsTabFolder, SWT.NONE);
//						measurementTabItem.setText(" 1 ");
//						{
//							measurementsComposite = new Composite(measurementsTabFolder, SWT.NONE);
//							measurementsComposite.setLayout(null);
//							measurementTabItem.setControl(measurementsComposite);
//							{
//								measurementNameLabel = new Label(measurementsComposite, SWT.RIGHT);
//								measurementNameLabel.setText("name");
//								measurementNameLabel.setBounds(10, 37, 60, 20);
//							}
//							{
//								measurementNameText = new Text(measurementsComposite, SWT.BORDER);
//								measurementNameText.setBounds(80, 37, 145, 20);
//							}
//							{
//								measurementSymbolLabel = new Label(measurementsComposite, SWT.RIGHT);
//								measurementSymbolLabel.setText("symbol");
//								measurementSymbolLabel.setBounds(10, 62, 60, 20);
//							}
//							{
//								measurementSymbolText = new Text(measurementsComposite, SWT.BORDER);
//								measurementSymbolText.setBounds(80, 62, 145, 20);
//							}
//							{
//								measurementUnitLabel = new Label(measurementsComposite, SWT.RIGHT);
//								measurementUnitLabel.setText("unit");
//								measurementUnitLabel.setBounds(10, 87, 60, 20);
//							}
//							{
//								measurementUnitText = new Text(measurementsComposite, SWT.BORDER);
//								measurementUnitText.setBounds(80, 87, 145, 20);
//							}
//							{
//								measurementEnableLabel = new Label(measurementsComposite, SWT.RIGHT);
//								measurementEnableLabel.setText("active");
//								measurementEnableLabel.setBounds(10, 112, 60, 20);
//							}
//							{
//								measurementEnableButton = new Button(measurementsComposite, SWT.CHECK);
//								measurementEnableButton.setBounds(82, 112, 145, 20);
//							}
//							{
//								channelConfigMeasurementPropertiesTabFolder = new CTabFolder(measurementsComposite, SWT.BORDER);
//								channelConfigMeasurementPropertiesTabFolder.setBounds(237, 0, 379, 199);
//								{
//									measurementPropertiesTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
//									measurementPropertiesTabItem.setShowClose(true);
//									measurementPropertiesTabItem.setText("Properties");
//									{
//										measurementsPropertiesTabFolder = new CTabFolder(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
//										measurementPropertiesTabItem.setControl(measurementsPropertiesTabFolder);
//										{
//											measurementPropertyTabItem = new CTabItem(measurementsPropertiesTabFolder, SWT.NONE);
//											measurementPropertyTabItem.setShowClose(true);
//											measurementPropertyTabItem.setText("Property");
//											{
//												MeasurementPropertiesComposite = new PropertyTypeComposite(measurementsPropertiesTabFolder, SWT.NONE);
//												measurementPropertyTabItem.setControl(MeasurementPropertiesComposite);
//											}
//										}
//										measurementsPropertiesTabFolder.setSelection(0);
//									}
//								}
//								{
//									measurementStatisticsTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
//									measurementStatisticsTabItem.setText("Statistics");
//									{
//										statisticsComposite = new StatisticsComposite(channelConfigMeasurementPropertiesTabFolder);
//										measurementStatisticsTabItem.setControl(statisticsComposite);
//
//									}
//								}
//								channelConfigMeasurementPropertiesTabFolder.setSelection(0);
//							}
//							{
//								label1 = new Label(measurementsComposite, SWT.NONE);
//								label1.setText("measurement");
//								label1.setBounds(10, 8, 120, 20);
//							}
//							{
//								addMeasurementButton = new Button(measurementsComposite, SWT.PUSH | SWT.CENTER);
//								addMeasurementButton.setText("+");
//								addMeasurementButton.setBounds(182, 7, 40, 20);
//							}
//						}
//					}
//					measurementsTabFolder.setSelection(0);
//				}
				{
					channelConfigAddButton = new Button(channelConfigComposite, SWT.PUSH | SWT.CENTER);
					channelConfigAddButton.setText("+");
					channelConfigAddButton.setBounds(574, 9, 42, 19);
					channelConfigAddButton.setToolTipText("add a new channel or configuration, this will inherit all definitions from precessor");
					channelConfigAddButton.setSize(40, 20);
					channelConfigAddButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("channelConfigAddButton.widgetSelected, event="+evt);
							//TODO add your code for channelConfigAddButton.widgetSelected
						}
					});
				}
			}
				}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void update() {
		this.channelConfigComposite.redraw();
	}
	
	public void clean() {
		//TODO cleanup measurements and properties
	}
}
