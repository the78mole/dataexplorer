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
	final static Logger		log	= Logger.getLogger(MeasurementTypeTabItem.class.getName());

	final CTabFolder			measurementsTabFolder;
	final String					tabName;

	Composite							measurementsComposite;
	Label									measurementNameLabel, measurementSymbolLabel, measurementUnitLabel, measurementEnableLabel, measurementCalculationLabel;
	Text									measurementNameText, measurementSymbolText, measurementUnitText;
	Button								measurementActiveButton, measurementCalculationButton;
	CTabFolder						channelConfigMeasurementPropertiesTabFolder;
	Button								addMeasurementButton;
	Label									measurementTypeLabel;

	String								measurementName, measurementSymbol, measurementUnit;
	boolean								isMeasurementActive, isMeasurementCalculation;

	CTabFolder						measurementsPropertiesTabFolder;
	CTabItem							measurementPropertiesTabItem;
	StatisticsTypeTabItem	statisticsTypeTabItem;

	DeviceConfiguration		deviceConfig;
	int										channelConfigNumber;
	MeasurementType				measurementType;

	public MeasurementTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		this.measurementsTabFolder = parent;
		this.tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		MeasurementTypeTabItem.log.log(Level.FINE, "MeasurementTypeTabItem " + this.tabName);
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 */
	public void setMeasurementType(DeviceConfiguration useDeviceConfig, MeasurementType useMeasurementType, int useChannelConfigNumber) {
		this.deviceConfig = useDeviceConfig;
		this.measurementType = useMeasurementType;
		this.channelConfigNumber = useChannelConfigNumber;

		this.measurementNameText.setText(this.measurementName = this.measurementType.getName());
		this.measurementSymbolText.setText(this.measurementSymbol = this.measurementType.getSymbol());
		this.measurementUnitText.setText(this.measurementUnit = this.measurementType.getUnit());
		this.measurementActiveButton.setSelection(this.isMeasurementActive = this.measurementType.isActive());
		this.measurementCalculationButton.setSelection(this.isMeasurementCalculation = this.measurementType.isCalculation());
		this.measurementActiveButton.setEnabled(!this.isMeasurementCalculation);

		this.setText(this.tabName + this.measurementName);
		this.measurementsComposite.redraw();

		int propertyCount = this.measurementsPropertiesTabFolder != null ? this.measurementsPropertiesTabFolder.getItemCount() : 0;
		int measurementPropertyCount = this.measurementType.getProperty().size();
		if (propertyCount < measurementPropertyCount) {
			for (int i = propertyCount; i < measurementPropertyCount; i++) {
				new PropertyTypeTabItem(this.measurementsPropertiesTabFolder, SWT.CLOSE, "?");
			}
		}
		else if (propertyCount > measurementPropertyCount) {
			for (int i = measurementPropertyCount; i < propertyCount; i++) {
				this.measurementsPropertiesTabFolder.getItem(0).dispose();
			}
		}
		String[] nameComboItems = StringHelper.enumValues2StringArray(MeasurementPropertyTypes.values());
		for (int i = 0; i < measurementPropertyCount; i++) {
			PropertyTypeTabItem tabItem = (PropertyTypeTabItem) this.measurementsPropertiesTabFolder.getItem(i);
			tabItem.setProperty(this.deviceConfig, this.measurementType.getProperty().get(i), true, true, false, true);
			tabItem.setNameComboItems(nameComboItems);
		}

		if (this.measurementType.getStatistics() == null && this.statisticsTypeTabItem != null && !this.statisticsTypeTabItem.isDisposed()) {
			this.statisticsTypeTabItem.dispose();
		}
		else if (this.statisticsTypeTabItem == null || this.statisticsTypeTabItem.isDisposed()) {
			this.statisticsTypeTabItem = new StatisticsTypeTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE | SWT.H_SCROLL, "Statistics");
		}
		if (this.statisticsTypeTabItem != null) {
			this.statisticsTypeTabItem.setStatisticsType(this.deviceConfig, this.measurementType.getStatistics(), this.channelConfigNumber);
		}
	}

	public MeasurementTypeTabItem(CTabFolder parent, int style, int index, MeasurementType useMeasurementType) {
		super(parent, style, index);
		this.measurementsTabFolder = parent;
		this.tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		this.measurementType = useMeasurementType;
		initGUI();
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			{
				this.measurementsComposite = new Composite(this.measurementsTabFolder, SWT.NONE);
				this.measurementsComposite.setLayout(null);
				this.setControl(this.measurementsComposite);
				{
					this.measurementTypeLabel = new Label(this.measurementsComposite, SWT.NONE);
					this.measurementTypeLabel.setText("measurement");
					this.measurementTypeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementTypeLabel.setBounds(10, 10, 120, 20);
				}
				{
					this.addMeasurementButton = new Button(this.measurementsComposite, SWT.PUSH | SWT.CENTER);
					this.addMeasurementButton.setText("+");
					this.addMeasurementButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.addMeasurementButton.setBounds(180, 10, 40, 20);
					this.addMeasurementButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINEST, "addMeasurementButton.widgetSelected, event=" + evt);
							new MeasurementTypeTabItem(MeasurementTypeTabItem.this.measurementsTabFolder, SWT.CLOSE, MeasurementTypeTabItem.this.measurementsTabFolder.getItemCount());
						}
					});
				}
				{
					this.measurementNameLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementNameLabel.setText("name");
					this.measurementNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementNameLabel.setBounds(10, 40, 60, 20);
				}
				{
					this.measurementNameText = new Text(this.measurementsComposite, SWT.BORDER);
					this.measurementNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementNameText.setBounds(80, 40, 145, 20);
					this.measurementNameText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINEST, "measurementNameText.keyReleased, event=" + evt);
							MeasurementTypeTabItem.this.measurementName = MeasurementTypeTabItem.this.measurementNameText.getText();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setName(MeasurementTypeTabItem.this.measurementName);
							}
						}
					});
				}
				{
					this.measurementSymbolLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementSymbolLabel.setText("symbol");
					this.measurementSymbolLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementSymbolLabel.setBounds(10, 65, 60, 20);
				}
				{
					this.measurementSymbolText = new Text(this.measurementsComposite, SWT.BORDER);
					this.measurementSymbolText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementSymbolText.setBounds(80, 65, 40, 20);
					this.measurementSymbolText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINEST, "measurementSymbolText.keyReleased, event=" + evt);
							MeasurementTypeTabItem.this.measurementSymbol = MeasurementTypeTabItem.this.measurementSymbolText.getText();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setSymbol(MeasurementTypeTabItem.this.measurementSymbol);
							}
						}
					});
				}
				{
					this.measurementUnitLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementUnitLabel.setText("unit");
					this.measurementUnitLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementUnitLabel.setBounds(10, 90, 60, 20);
				}
				{
					this.measurementUnitText = new Text(this.measurementsComposite, SWT.BORDER);
					this.measurementUnitText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementUnitText.setBounds(80, 90, 40, 20);
					this.measurementUnitText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINEST, "measurementUnitText.keyReleased, event=" + evt);
							MeasurementTypeTabItem.this.measurementUnit = MeasurementTypeTabItem.this.measurementUnitText.getText();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setUnit(MeasurementTypeTabItem.this.measurementUnit);
							}
						}
					});
				}
				{
					this.measurementEnableLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementEnableLabel.setText("active");
					this.measurementEnableLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementEnableLabel.setBounds(3, 115, 67, 20);
				}
				{
					this.measurementActiveButton = new Button(this.measurementsComposite, SWT.CHECK);
					this.measurementActiveButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementActiveButton.setBounds(80, 115, 145, 20);
					this.measurementActiveButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINEST, "measurementEnableButton.widgetSelected, event=" + evt);
							MeasurementTypeTabItem.this.isMeasurementActive = MeasurementTypeTabItem.this.measurementActiveButton.getSelection();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setActive(MeasurementTypeTabItem.this.isMeasurementActive);
							}
						}
					});
				}
				{
					this.measurementCalculationLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementCalculationLabel.setText("calculation");
					this.measurementCalculationLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementCalculationLabel.setBounds(3, 140, 67, 20);
				}
				{
					this.measurementCalculationButton = new Button(this.measurementsComposite, SWT.CHECK);
					this.measurementCalculationButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.measurementCalculationButton.setBounds(80, 140, 145, 20);
					this.measurementCalculationButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINEST, "measurementCalculationButton.widgetSelected, event=" + evt);
							MeasurementTypeTabItem.this.isMeasurementCalculation = MeasurementTypeTabItem.this.measurementCalculationButton.getSelection();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setActive(MeasurementTypeTabItem.this.isMeasurementCalculation);
							}
							MeasurementTypeTabItem.this.measurementActiveButton.setEnabled(!MeasurementTypeTabItem.this.isMeasurementCalculation);
						}
					});
				}
				{
					this.channelConfigMeasurementPropertiesTabFolder = new CTabFolder(this.measurementsComposite, SWT.BORDER);
					this.channelConfigMeasurementPropertiesTabFolder.setBounds(237, 0, 379, 199);
					{
						this.statisticsTypeTabItem = new StatisticsTypeTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE | SWT.H_SCROLL, "Statistics");
					}
					{
						this.measurementPropertiesTabItem = new CTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
						this.measurementPropertiesTabItem.setText("Properties");
						this.measurementPropertiesTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						{
							this.measurementsPropertiesTabFolder = new CTabFolder(this.channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
							this.measurementPropertiesTabItem.setControl(this.measurementsPropertiesTabFolder);
							{
								//PropertyTypeTabItem tabItem = 
								new PropertyTypeTabItem(this.measurementsPropertiesTabFolder,	SWT.CLOSE, "offset");
										//, "offset", DataTypes.DOUBLE, 0.0, "offset to measurement value, applied after factor");
								//tabItem.setProperty(this.measurementType.getProperty().get(i), true, true, false, true);
								new PropertyTypeTabItem(this.measurementsPropertiesTabFolder, SWT.CLOSE, "factor"); 
								//, "factor", DataTypes.DOUBLE, 1.0, "factor to measurement value, applied after reduction");
								new PropertyTypeTabItem(this.measurementsPropertiesTabFolder, SWT.CLOSE, "reduction");
								//, "reduction", DataTypes.DOUBLE, 0.0,	"direct reduction to measurement value, applied before factor");
							}
							this.measurementsPropertiesTabFolder.setSelection(0);
							this.measurementsPropertiesTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
								@Override
								public void restore(CTabFolderEvent evt) {
									MeasurementTypeTabItem.log.log(Level.FINE, "measurementsPropertiesTabFolder.restore, event=" + evt);
									((CTabItem) evt.item).getControl();
								}

								@Override
								public void close(CTabFolderEvent evt) {
									MeasurementTypeTabItem.log.log(Level.FINE, "measurementsPropertiesTabFolder.close, event=" + evt);
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
					this.channelConfigMeasurementPropertiesTabFolder.setSelection(0);
					this.channelConfigMeasurementPropertiesTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
						@Override
						public void restore(CTabFolderEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINE, "channelConfigMeasurementPropertiesTabFolder.restore, event=" + evt);
							((CTabItem) evt.item).getControl();
						}

						@Override
						public void close(CTabFolderEvent evt) {
							MeasurementTypeTabItem.log.log(Level.FINE, "channelConfigMeasurementPropertiesTabFolder.close, event=" + evt);
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
				this.measurementsComposite.layout();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
