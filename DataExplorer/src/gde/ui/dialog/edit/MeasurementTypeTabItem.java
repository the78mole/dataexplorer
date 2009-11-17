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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.DeviceConfiguration;
import osde.device.MeasurementPropertyTypes;
import osde.device.MeasurementType;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * class defining a CTabItem with MeasurementType configuration data
 * @author Winfried Brügmann
 */
public class MeasurementTypeTabItem extends CTabItem {
	final static Logger						log			= Logger.getLogger(MeasurementTypeTabItem.class.getName());

	final CTabFolder measurementsTabFolder;
	final String tabName;
	
	Composite measurementsComposite;
	Label measurementNameLabel, measurementSymbolLabel, measurementUnitLabel, measurementEnableLabel, measurementCalculationLabel;
	Text measurementNameText, measurementSymbolText, measurementUnitText;
	Button measurementActiveButton, measurementCalculationButton;
	CTabFolder channelConfigMeasurementPropertiesTabFolder;
	Button addMeasurementButton;
	Label measurementTypeLabel;
	
	String measurementName, measurementSymbol, measurementUnit;
	boolean isMeasurementActive, isMeasurementCalculation;
	
	CTabFolder measurementsPropertiesTabFolder;
	CTabItem measurementPropertiesTabItem;
	StatisticsTypeTabItem statisticsTypeTabItem;

	DeviceConfiguration deviceConfig;
	int channelConfigNumber;
	MeasurementType measurementType;
	
	
	public MeasurementTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		measurementsTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		System.out.println("MeasurementTypeTabItem " + tabName);
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 */
	public void setMeasurementType(DeviceConfiguration useDeviceConfig, MeasurementType useMeasurementType, int useChannelConfigNumber) {
		this.deviceConfig = useDeviceConfig;
		this.measurementType = useMeasurementType;
		this.channelConfigNumber = useChannelConfigNumber;
		
		measurementNameText.setText(measurementName = measurementType.getName());
		measurementSymbolText.setText(measurementSymbol = measurementType.getSymbol());
		measurementUnitText.setText(measurementUnit = measurementType.getUnit());
		measurementActiveButton.setSelection(isMeasurementActive = measurementType.isActive());
		measurementCalculationButton.setSelection(isMeasurementCalculation = measurementType.isCalculation());
		measurementActiveButton.setEnabled(!isMeasurementCalculation);
		
		measurementsComposite.redraw();
		
		int propertyCount = measurementsPropertiesTabFolder != null ? measurementsPropertiesTabFolder.getItemCount() : 0;
		int measurementPropertyCount = measurementType.getProperty().size();
		if (propertyCount < measurementPropertyCount) {
			for (int i = propertyCount; i < measurementPropertyCount; i++) {
				new PropertyTypeTabItem(measurementsPropertiesTabFolder, SWT.CLOSE, "?");
			}
		}
		else if (propertyCount > measurementPropertyCount) {
			for (int i = measurementPropertyCount; i < propertyCount; i++) {
				measurementsPropertiesTabFolder.getItem(0).dispose();
			}
		}
		String[] nameComboItems = StringHelper.enumValues2StringArray(MeasurementPropertyTypes.values());
		for (int i = 0; i < measurementPropertyCount; i++) {
			PropertyTypeTabItem tabItem = (PropertyTypeTabItem)measurementsPropertiesTabFolder.getItem(i);
			tabItem.setProperty(measurementType.getProperty().get(i), true, true, false, true);
			tabItem.setNameComboItems(nameComboItems);
		}
		
		if (measurementType.getStatistics() == null && statisticsTypeTabItem != null && !statisticsTypeTabItem.isDisposed()) {
			statisticsTypeTabItem.dispose();
		}
		else if (statisticsTypeTabItem == null || statisticsTypeTabItem.isDisposed()) {
			statisticsTypeTabItem = new StatisticsTypeTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE | SWT.H_SCROLL, "Statistics");
		}
		if (statisticsTypeTabItem != null) {
			statisticsTypeTabItem.setStatisticsType(deviceConfig, measurementType.getStatistics(), channelConfigNumber);
		}
	}
	
	public MeasurementTypeTabItem(CTabFolder parent, int style, int index, MeasurementType useMeasurementType) {
		super(parent, style, index);
		measurementsTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		this.measurementType = useMeasurementType;
		initGUI();
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			{
				measurementsComposite = new Composite(measurementsTabFolder, SWT.NONE);
				measurementsComposite.setLayout(null);
				this.setControl(measurementsComposite);
				{
					measurementTypeLabel = new Label(measurementsComposite, SWT.NONE);
					measurementTypeLabel.setText("measurement");
					measurementTypeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementTypeLabel.setBounds(10, 10, 120, 20);
				}
				{
					addMeasurementButton = new Button(measurementsComposite, SWT.PUSH | SWT.CENTER);
					addMeasurementButton.setText("+");
					addMeasurementButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					addMeasurementButton.setBounds(180, 10, 40, 20);
					addMeasurementButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addMeasurementButton.widgetSelected, event="+evt);
							new MeasurementTypeTabItem(measurementsTabFolder, SWT.CLOSE, measurementsTabFolder.getItemCount());
						}
					});
				}
				{
					measurementNameLabel = new Label(measurementsComposite, SWT.RIGHT);
					measurementNameLabel.setText("name");
					measurementNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementNameLabel.setBounds(10, 40, 60, 20);
				}
				{
					measurementNameText = new Text(measurementsComposite, SWT.BORDER);
					measurementNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementNameText.setBounds(80, 40, 145, 20);
					measurementNameText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "measurementNameText.keyReleased, event="+evt);
							measurementName = measurementNameText.getText();
							if (measurementType != null) {
								measurementType.setName(measurementName);
							}
						}
					});
				}
				{
					measurementSymbolLabel = new Label(measurementsComposite, SWT.RIGHT);
					measurementSymbolLabel.setText("symbol");
					measurementSymbolLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementSymbolLabel.setBounds(10, 65, 60, 20);
				}
				{
					measurementSymbolText = new Text(measurementsComposite, SWT.BORDER);
					measurementSymbolText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementSymbolText.setBounds(80, 65, 40, 20);
					measurementSymbolText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "measurementSymbolText.keyReleased, event="+evt);
							measurementSymbol = measurementSymbolText.getText();
							if (measurementType != null) {
								measurementType.setSymbol(measurementSymbol);
							}
						}
					});
				}
				{
					measurementUnitLabel = new Label(measurementsComposite, SWT.RIGHT);
					measurementUnitLabel.setText("unit");
					measurementUnitLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementUnitLabel.setBounds(10, 90, 60, 20);
				}
				{
					measurementUnitText = new Text(measurementsComposite, SWT.BORDER);
					measurementUnitText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementUnitText.setBounds(80, 90, 40, 20);
					measurementUnitText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "measurementUnitText.keyReleased, event="+evt);
							measurementUnit = measurementUnitText.getText();
							if (measurementType != null) {
								measurementType.setUnit(measurementUnit);
							}
						}
					});
				}
				{
					measurementEnableLabel = new Label(measurementsComposite, SWT.RIGHT);
					measurementEnableLabel.setText("active");
					measurementEnableLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementEnableLabel.setBounds(3, 115, 67, 20);
				}
				{
					measurementActiveButton = new Button(measurementsComposite, SWT.CHECK);
					measurementActiveButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementActiveButton.setBounds(80, 115, 145, 20);
					measurementActiveButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "measurementEnableButton.widgetSelected, event="+evt);
							isMeasurementActive = measurementActiveButton.getSelection();
							if (measurementType != null) {
								measurementType.setActive(isMeasurementActive);
							}
						}
					});
				}
				{
					measurementCalculationLabel = new Label(measurementsComposite, SWT.RIGHT);
					measurementCalculationLabel.setText("calculation");
					measurementCalculationLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementCalculationLabel.setBounds(3, 140, 67, 20);
				}
				{
					measurementCalculationButton = new Button(measurementsComposite, SWT.CHECK);
					measurementCalculationButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					measurementCalculationButton.setBounds(80, 140, 145, 20);
					measurementCalculationButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "measurementCalculationButton.widgetSelected, event="+evt);
							isMeasurementCalculation = measurementCalculationButton.getSelection();
							if (measurementType != null) {
								measurementType.setActive(isMeasurementCalculation);
							}
							measurementActiveButton.setEnabled(!isMeasurementCalculation);
						}
					});
				}
				{
					channelConfigMeasurementPropertiesTabFolder = new CTabFolder(measurementsComposite, SWT.BORDER);
					channelConfigMeasurementPropertiesTabFolder.setBounds(237, 0, 379, 199);
					{
						statisticsTypeTabItem = new StatisticsTypeTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE | SWT.H_SCROLL, "Statistics");
					}
					{
						measurementPropertiesTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
						measurementPropertiesTabItem.setText("Properties");
						measurementPropertiesTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						{
							measurementsPropertiesTabFolder = new CTabFolder(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
							measurementPropertiesTabItem.setControl(measurementsPropertiesTabFolder);
							{
								new PropertyTypeTabItem(measurementsPropertiesTabFolder, SWT.CLOSE, "offset", "offset", DataTypes.DOUBLE, 0.0, "offset to measurement value, applied after factor");
								new PropertyTypeTabItem(measurementsPropertiesTabFolder, SWT.CLOSE, "factor", "factor", DataTypes.DOUBLE, 1.0, "factor to measurement value, applied after reduction");
								new PropertyTypeTabItem(measurementsPropertiesTabFolder, SWT.CLOSE, "reduction", "reduction", DataTypes.DOUBLE, 0.0, "direct reduction to measurement value, applied before factor");
							}
							measurementsPropertiesTabFolder.setSelection(0);
							measurementsPropertiesTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
								public void restore(CTabFolderEvent evt) {
									System.out.println("measurementsPropertiesTabFolder.restore, event="+evt);
									((CTabItem)evt.item).getControl();
								}
								public void close(CTabFolderEvent evt) {
									System.out.println("measurementsPropertiesTabFolder.close, event="+evt);
//									CTabItem tabItem = ((CTabItem)evt.item);
//									if (deviceConfig != null) {
//										if (tabItem.getText().equals("State")) deviceConfig.removeStateType();
//										else if (tabItem.getText().equals("Serial Port")) deviceConfig.removeSerialPortType();
//										else if (tabItem.getText().equals("Data Block")) deviceConfig.removeDataBlockType();
//									}
//									tabItem.dispose();
//									if(deviceConfig != null) 
//										update();
								}
							});
						}
					}
					channelConfigMeasurementPropertiesTabFolder.setSelection(0);
					channelConfigMeasurementPropertiesTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
						public void restore(CTabFolderEvent evt) {
							System.out.println("channelConfigMeasurementPropertiesTabFolder.restore, event="+evt);
							((CTabItem)evt.item).getControl();
						}
						public void close(CTabFolderEvent evt) {
							System.out.println("channelConfigMeasurementPropertiesTabFolder.close, event="+evt);
//							CTabItem tabItem = ((CTabItem)evt.item);
//							if (deviceConfig != null) {
//								if (tabItem.getText().equals("State")) deviceConfig.removeStateType();
//								else if (tabItem.getText().equals("Serial Port")) deviceConfig.removeSerialPortType();
//								else if (tabItem.getText().equals("Data Block")) deviceConfig.removeDataBlockType();
//							}
//							tabItem.dispose();
//							if(deviceConfig != null) 
//								update();
						}
					});
				}
				measurementsComposite.layout();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
