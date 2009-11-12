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
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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

/**
 * class defining a CTabItem with ChannelType configuration data
 * @author Winfried Brügmann
 */
public class ChannelTypeTabItem extends CTabItem {
	final static Logger						log			= Logger.getLogger(ChannelTypeTabItem.class.getName());

	Composite channelConfigComposite;
	Button channelConfigAddButton;
	Label channelConfigLabel;
	CCombo channelConfigTypeCombo;
	Text channelConfigText;
	CTabFolder measurementsTabFolder;

	
	ChannelTypes channelConfigType = ChannelTypes.TYPE_OUTLET;
	String channelConfigName = "Outlet";

	final CTabFolder channelConfigInnerTabFolder;
	final String tabName;
	ChannelType channelType; 
	
	
	public ChannelTypeTabItem(CTabFolder parent, int index) {
		super(parent, SWT.NONE);
		channelConfigInnerTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index+1) + OSDE.STRING_BLANK;
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 */
	public void setChannelType(ChannelType useChannelType) {
		this.channelType = useChannelType;
		this.channelConfigComposite.redraw();
		
		//MeasurementType begin
		int measurementTypeCount = channelType.getMeasurement().size();
		int actualTabItemCount = measurementsTabFolder.getItemCount();
		if (measurementTypeCount < actualTabItemCount) {
			for (int i = measurementTypeCount; i < actualTabItemCount; i++) {
				MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem)measurementsTabFolder.getItem(measurementTypeCount);
				measurementTabItem.dispose();
			}
		}
		else if (measurementTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < measurementTypeCount; i++) {
				new MeasurementTypeTabItem(measurementsTabFolder, i);
			}
		}
		for (int i = 0; i < measurementTypeCount; i++) {
			MeasurementTypeTabItem measurementTabItem = (MeasurementTypeTabItem)measurementsTabFolder.getItem(i);
			measurementTabItem.setMeasurementType(channelType.getMeasurement().get(i));
		}
		//MeasurementType begin
	}
	
	public ChannelTypeTabItem(CTabFolder parent, int index, ChannelType useChannelType) {
		super(parent, SWT.NONE);
		channelConfigInnerTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		this.channelType = useChannelType;
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
						log.log(Level.FINEST, "channelConfigComposite.paintControl, event=" + evt);
						if (channelType != null) {
							channelConfigType = channelType.getType();
							channelConfigTypeCombo.select(channelConfigType.ordinal());
							channelConfigName = channelType.getName();
							channelConfigText.setText(channelConfigName);
						}
					}
				});
				{
					channelConfigTypeCombo = new CCombo(channelConfigComposite, SWT.BORDER);
					channelConfigTypeCombo.setBounds(6, 9, 121, 20);
					channelConfigTypeCombo.setItems(new String[] { "TYPE_OUTLET", "TYPE_CONFIG" });
					channelConfigTypeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "channelConfigTypeCombo.widgetSelected, event=" + evt);
							channelConfigType = ChannelTypes.valueOf(channelConfigTypeCombo.getText());
							if (channelType != null) {
								channelType.setType(channelConfigType);
							}
						}
					});
				}
				{
					channelConfigText = new Text(channelConfigComposite, SWT.BORDER);
					channelConfigText.setText("Outlet");
					channelConfigText.setBounds(147, 9, 128, 20);
					channelConfigText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "channelConfigText.keyReleased, event=" + evt);
							channelConfigName = channelConfigText.getText().trim();
							if (channelType != null) {
								channelType.setName(channelConfigName);
							}
						}
					});
				}
				{
					channelConfigLabel = new Label(channelConfigComposite, SWT.CENTER);
					channelConfigLabel.setText("complete definitions before adding new");
					channelConfigLabel.setBounds(289, 9, 279, 20);
				}
				{
					measurementsTabFolder = new CTabFolder(channelConfigComposite, SWT.CLOSE | SWT.BORDER);
					measurementsTabFolder.setBounds(0, 35, 622, 225);
					{
						//create initial measurement type
						new MeasurementTypeTabItem(measurementsTabFolder, SWT.NONE);
					}
					measurementsTabFolder.setSelection(0);
				}
				{
					channelConfigAddButton = new Button(channelConfigComposite, SWT.PUSH | SWT.CENTER);
					channelConfigAddButton.setText("+");
					channelConfigAddButton.setBounds(574, 9, 42, 19);
					channelConfigAddButton.setToolTipText("add a new channel or configuration, this will inherit all definitions from precessor");
					channelConfigAddButton.setSize(40, 20);
					channelConfigAddButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "channelConfigAddButton.widgetSelected, event=" + evt);
							new ChannelTypeTabItem(channelConfigInnerTabFolder, channelConfigInnerTabFolder.getItemCount());
						}
					});
				}
			}
			this.addDisposeListener(new DisposeListener() {	
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "ChannelTypeTabItem.widgetDisposed, event=" + evt);
//				for (CTabItem tabItem : measurementsTabFolder.getItems()) {
//				MeasurementTypeTabItem mtabItem = ((MeasurementTypeTabItem)tabItem);
//				mtabItem.dispose();
//			}
					
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
