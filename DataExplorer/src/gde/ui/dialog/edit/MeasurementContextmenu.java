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
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;

import osde.device.MeasurementPropertyTypes;

/**
 * Class to represent the context menu to enable adding statistics and property tab items to measurement
 * @author Winfried Brügmann
 */
public class MeasurementContextmenu {
	final static Logger					log												= Logger.getLogger(ContextMenu.class.getName());

	final Menu		 menu;
	final MeasurementTypeTabItem measurementTypeTabItem;
	final CTabFolder						channelConfigMeasurementPropertiesTabFolder;
	CTabFolder						measurementPropertiesTabFolder;
		
	MenuItem addStatisticsTypeMenuItem, addPropertyTypeMenuItem;
	Menu addPropertyTypeMenu;
	MenuItem defaultPropertyMenuItem;
	MenuItem offsetPropertyMenuItem, factorPropertyMenuItem, reductionPropertyMenuItem;
	MenuItem regressionIntervalPropertyMenuItem, regressionTypeCurvePropertyMenuItem, regressionTypeLinearPropertyMenuItem;
	MenuItem numberMotorPropertyMenuItem, revolutionFactorPropertyMenuItem, prop100WPropertyMenuItem;
	MenuItem numberCellsPropertyMenuItem, invertCurrentPropertyMenuItem;


	public MeasurementContextmenu(Menu useMenu, MeasurementTypeTabItem parent, CTabFolder useChannelConfigMeasurementPropertiesTabFolder) {
		menu = useMenu;
		measurementTypeTabItem = parent;
		channelConfigMeasurementPropertiesTabFolder = useChannelConfigMeasurementPropertiesTabFolder;
	}	

	public void create() {
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				log.log(Level.FINEST, "menuShown action performed! " + e); //$NON-NLS-1$
				addStatisticsTypeMenuItem.setEnabled(true);
				addPropertyTypeMenuItem.setEnabled(true);
				for (CTabItem tabItem : channelConfigMeasurementPropertiesTabFolder.getItems()) {
					if (tabItem.getText().equals("Statistics")) {
						addStatisticsTypeMenuItem.setEnabled(false);
					}
				}
				//refresh tab folder it could be destroyed and re-created
				measurementPropertiesTabFolder = measurementTypeTabItem.measurementPropertiesTabFolder;
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		this.addStatisticsTypeMenuItem = new MenuItem(menu, SWT.PUSH);
		this.addStatisticsTypeMenuItem.setText("add StatisticsType");
		this.addStatisticsTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addStatisticsTypeMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createStatisticsTabItem();
			}
		});
		new MenuItem(menu, SWT.SEPARATOR);
		this.addPropertyTypeMenuItem = new MenuItem(menu, SWT.CASCADE);
		this.addPropertyTypeMenuItem.setText("add PropertyType");
		this.addPropertyTypeMenu = new Menu(this.addPropertyTypeMenuItem);
		this.addPropertyTypeMenuItem.setMenu(this.addPropertyTypeMenu);
		this.addPropertyTypeMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				log.log(Level.FINEST, "addPropertyTypeMenu.menuShown action performed! " + e); //$NON-NLS-1$
				defaultPropertyMenuItem.setEnabled(true);
				offsetPropertyMenuItem.setEnabled(true);
				factorPropertyMenuItem.setEnabled(true);
				reductionPropertyMenuItem.setEnabled(true);
				regressionIntervalPropertyMenuItem.setEnabled(true);
				regressionTypeCurvePropertyMenuItem.setEnabled(true);
				regressionTypeLinearPropertyMenuItem.setEnabled(true);
				numberMotorPropertyMenuItem.setEnabled(true);
				revolutionFactorPropertyMenuItem.setEnabled(true);
				prop100WPropertyMenuItem.setEnabled(true);
				numberCellsPropertyMenuItem.setEnabled(true);
				invertCurrentPropertyMenuItem.setEnabled(true);
				if (measurementPropertiesTabFolder != null) {
					for (CTabItem tabItem : measurementPropertiesTabFolder.getItems()) {
						try {
							switch (MeasurementPropertyTypes.fromValue(tabItem.getText())) {
							case OFFSET:
								offsetPropertyMenuItem.setEnabled(false);
								break;
							case FACTOR:
								factorPropertyMenuItem.setEnabled(false);
								break;
							case REDUCTION:
								reductionPropertyMenuItem.setEnabled(false);
								break;
							case REGRESSION_INTERVAL_SEC:
								regressionIntervalPropertyMenuItem.setEnabled(false);
								break;
							case REGRESSION_TYPE_CURVE:
								regressionTypeCurvePropertyMenuItem.setEnabled(false);
								break;
							case REGRESSION_TYPE_LINEAR:
								regressionTypeLinearPropertyMenuItem.setEnabled(false);
								break;
							case NUMBER_MOTOR:
								numberMotorPropertyMenuItem.setEnabled(false);
								break;
							case REVOLUTION_FACTOR:
								revolutionFactorPropertyMenuItem.setEnabled(false);
								break;
							case NUMBER_CELLS:
								numberCellsPropertyMenuItem.setEnabled(false);
								break;
							case PROP_N_100_W:
								prop100WPropertyMenuItem.setEnabled(false);
								break;
							case IS_INVERT_CURRENT:
								invertCurrentPropertyMenuItem.setEnabled(false);
								break;
							case NONE_SPECIFIED:
								MessageBox mb = new MessageBox(menu.getShell(), SWT.OK);
								mb.setText("Warning");
								mb.setMessage("The existing none_specified measurement PropertyType must be renamed before another none_specified PropertyItem can be created!");
								mb.open();
								break;
							}
						}
						catch (Exception e1) {
							//ignore updated none_specified PropertyTypes
						}
					}
				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});		
		this.defaultPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.defaultPropertyMenuItem.setText(MeasurementPropertyTypes.NONE_SPECIFIED.value());
		this.defaultPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "defaultPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.NONE_SPECIFIED.value());
			}
		});
		this.offsetPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.offsetPropertyMenuItem.setText(MeasurementPropertyTypes.OFFSET.value());
		this.offsetPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "defaultPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.OFFSET.value());
			}
		});
		this.factorPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.factorPropertyMenuItem.setText(MeasurementPropertyTypes.FACTOR.value());
		this.factorPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "factorPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.FACTOR.value());
			}
		});
		this.reductionPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.reductionPropertyMenuItem.setText(MeasurementPropertyTypes.REDUCTION.value());
		this.reductionPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "reductionPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REDUCTION.value());
			}
		});
		this.regressionIntervalPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.regressionIntervalPropertyMenuItem.setText(MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value());
		this.regressionIntervalPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "regressionIntervalPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value());
			}
		});
		this.regressionTypeCurvePropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.regressionTypeCurvePropertyMenuItem.setText(MeasurementPropertyTypes.REGRESSION_TYPE_CURVE.value());
		this.regressionTypeCurvePropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "regressionTypeCurvePropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REGRESSION_TYPE_CURVE.value());
			}
		});
		this.regressionTypeLinearPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.regressionTypeLinearPropertyMenuItem.setText(MeasurementPropertyTypes.REGRESSION_TYPE_LINEAR.value());
		this.regressionTypeLinearPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "regressionTypeLinearPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REGRESSION_TYPE_LINEAR.value());
			}
		});
		this.numberMotorPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.numberMotorPropertyMenuItem.setText(MeasurementPropertyTypes.NUMBER_MOTOR.value());
		this.numberMotorPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "numberMotorPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.NUMBER_MOTOR.value());
			}
		});
		this.revolutionFactorPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.revolutionFactorPropertyMenuItem.setText(MeasurementPropertyTypes.REVOLUTION_FACTOR.value());
		this.revolutionFactorPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "revolutionFactorPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.REVOLUTION_FACTOR.value());
			}
		});
		this.prop100WPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.prop100WPropertyMenuItem.setText(MeasurementPropertyTypes.PROP_N_100_W.value());
		this.prop100WPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "prop100WPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.PROP_N_100_W.value());
			}
		});
		this.numberCellsPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.numberCellsPropertyMenuItem.setText(MeasurementPropertyTypes.NUMBER_CELLS.value());
		this.numberCellsPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "numberCellsPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.NUMBER_CELLS.value());
			}
		});
		this.invertCurrentPropertyMenuItem = new MenuItem(addPropertyTypeMenu, SWT.PUSH);
		this.invertCurrentPropertyMenuItem.setText(MeasurementPropertyTypes.IS_INVERT_CURRENT.value());
		this.invertCurrentPropertyMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "invertCurrentPropertyMenuItem action performed! " + e); //$NON-NLS-1$
				measurementTypeTabItem.createMeasurementPropertyTabItem(MeasurementPropertyTypes.IS_INVERT_CURRENT.value());
			}
		});
	}

}
