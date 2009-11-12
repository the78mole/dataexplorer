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
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.MeasurementType;

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
//	Composite statisticsComposite;
//	Composite MeasurementPropertiesComposite;
//	CTabItem measurementPropertyTabItem;
//	CTabItem measurementStatisticsTabItem;
//	CTabItem measurementPropertiesTabItem;

	MeasurementType measurementType;
	
	
	public MeasurementTypeTabItem(CTabFolder parent, int index) {
		super(parent, SWT.NONE, index);
		measurementsTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		initGUI();
	}

	/**
	 * @param useDeviceConfig the deviceConfig to set
	 */
	public void setMeasurementType(MeasurementType useMeasurementType) {
		this.measurementType = useMeasurementType;
		
		measurementNameText.setText(measurementName = measurementType.getName());
		measurementSymbolText.setText(measurementSymbol = measurementType.getSymbol());
		measurementUnitText.setText(measurementUnit = measurementType.getUnit());
		measurementActiveButton.setSelection(isMeasurementActive = measurementType.isActive());
		measurementCalculationButton.setSelection(isMeasurementCalculation = measurementType.isCalculation());
		measurementActiveButton.setEnabled(!isMeasurementCalculation);
		
		measurementsComposite.redraw();
	}
	
	public MeasurementTypeTabItem(CTabFolder parent, int index, MeasurementType useMeasurementType) {
		super(parent, SWT.NONE, index);
		measurementsTabFolder = parent;
		tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		this.measurementType = useMeasurementType;
		initGUI();
	}

	private void initGUI() {
		try {
			this.setText(tabName);
			{
				measurementsComposite = new Composite(measurementsTabFolder, SWT.NONE);
				measurementsComposite.setLayout(null);
				this.setControl(measurementsComposite);
				{
					measurementTypeLabel = new Label(measurementsComposite, SWT.NONE);
					measurementTypeLabel.setText("measurement");
					measurementTypeLabel.setBounds(10, 10, 120, 20);
				}
				{
					addMeasurementButton = new Button(measurementsComposite, SWT.PUSH | SWT.CENTER);
					addMeasurementButton.setText("+");
					addMeasurementButton.setBounds(180, 10, 40, 20);
					addMeasurementButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addMeasurementButton.widgetSelected, event="+evt);
							new MeasurementTypeTabItem(measurementsTabFolder, measurementsTabFolder.getItemCount());
						}
					});
				}
				{
					measurementNameLabel = new Label(measurementsComposite, SWT.RIGHT);
					measurementNameLabel.setText("name");
					measurementNameLabel.setBounds(10, 40, 60, 20);
				}
				{
					measurementNameText = new Text(measurementsComposite, SWT.BORDER);
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
					measurementSymbolLabel.setBounds(10, 65, 60, 20);
				}
				{
					measurementSymbolText = new Text(measurementsComposite, SWT.BORDER);
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
					measurementUnitLabel.setBounds(10, 90, 60, 20);
				}
				{
					measurementUnitText = new Text(measurementsComposite, SWT.BORDER);
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
					measurementEnableLabel.setBounds(3, 115, 67, 20);
				}
				{
					measurementActiveButton = new Button(measurementsComposite, SWT.CHECK);
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
					measurementCalculationLabel.setBounds(3, 140, 67, 20);
				}
				{
					measurementCalculationButton = new Button(measurementsComposite, SWT.CHECK);
					measurementCalculationButton.setBounds(80, 140, 145, 20);
					measurementCalculationButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "measurementCalculationButton.widgetSelected, event="+evt);
							isMeasurementCalculation = measurementCalculationButton.getSelection();
							if (measurementType != null) {
								measurementType.setActive(isMeasurementCalculation ? isMeasurementActive : null);
							}
							measurementActiveButton.setEnabled(!isMeasurementCalculation);
						}
					});
				}
				{
					channelConfigMeasurementPropertiesTabFolder = new CTabFolder(measurementsComposite, SWT.BORDER);
					channelConfigMeasurementPropertiesTabFolder.setBounds(237, 0, 379, 199);
//					{
//						measurementPropertiesTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
//						measurementPropertiesTabItem.setShowClose(true);
//						measurementPropertiesTabItem.setText("Properties");
//						{
//							measurementsPropertiesTabFolder = new CTabFolder(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
//							measurementPropertiesTabItem.setControl(measurementsPropertiesTabFolder);
//							{
//								measurementPropertyTabItem = new CTabItem(measurementsPropertiesTabFolder, SWT.NONE);
//								measurementPropertyTabItem.setShowClose(true);
//								measurementPropertyTabItem.setText("Property");
//								{
//									MeasurementPropertiesComposite = new PropertyTypeComposite(measurementsPropertiesTabFolder, SWT.NONE);
//									measurementPropertyTabItem.setControl(MeasurementPropertiesComposite);
//								}
//							}
//							measurementsPropertiesTabFolder.setSelection(0);
//						}
//					}
//					{
//						measurementStatisticsTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
//						measurementStatisticsTabItem.setText("Statistics");
//						{
//							statisticsComposite = new StatisticsComposite(channelConfigMeasurementPropertiesTabFolder);
//							measurementStatisticsTabItem.setControl(statisticsComposite);
//
//						}
//					}
					channelConfigMeasurementPropertiesTabFolder.setSelection(0);
				}
			}
			this.addDisposeListener(new DisposeListener() {	
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "MeasurementTypeTabItem.widgetDisposed, event=" + evt);
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
