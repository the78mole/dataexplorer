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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.DesktopPropertyTypes;
import osde.device.DeviceConfiguration;
import osde.device.MeasurementPropertyTypes;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * Composite to wrap XML PropertyType enable to edit existing and create new device property files 
 * @author Winfried Brügmann
 */
public class PropertyTypeTabItem extends CTabItem {
	final static Logger						log	= Logger.getLogger(PropertyTypeTabItem.class.getName());

	final CTabFolder							parentTabFolder;
	final MeasurementTypeTabItem	measurementTypeTabItem;

	Composite								propertyTypeComposite;
	Label										nameLabel;
	Label										typeLabel;
	Label										valueLabel;
	Label										descriptionLabel;
	Text										nameText, valueText, descriptionText;
	CCombo									typeCombo, valueCombo, nameCombo;
	KeyAdapter							valueKeyListener;
	VerifyListener					valueVerifyListener;

	DeviceConfiguration			deviceConfig;
	boolean									isStateType;									//1000
	boolean									isValueOnlyEnabledType;				//0001
	boolean									isNameSelectionEnabledType;		//01**
	boolean									isTypeSelectionEnabledType;		//0*1*

	Menu										popupMenu;
	MeasurementContextmenu	contextMenu;
	String									tabName;
	PropertyType						propertyType;

	/**
	 * constructor without any variables input
	 * to display default values a call of update(PropertyType) is required
	 * @param parent must be a CTabFolder
	 * @param style
	 * @param useTabName
	 * @param useMeasurementTypeTabItem2CreatePopupMenu != null signal a popup menu should be initialized
	 */
	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, MeasurementTypeTabItem	useMeasurementTypeTabItem2CreatePopupMenu) {
		super(parent, style);
		this.parentTabFolder = parent;
		this.tabName = useTabName;
		this.measurementTypeTabItem = useMeasurementTypeTabItem2CreatePopupMenu;
		this.propertyType = new ObjectFactory().createPropertyType();
		this.propertyType.setName(Messages.getString(MessageIds.OSDE_MSGT0473));
		this.propertyType.setType(DataTypes.INTEGER);
		this.propertyType.setValue(OSDE.STRING_EMPTY + 0);
		this.propertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0474));

		initGUI();
	}

	/**
	 * enable the context menu to create missing tab items
	 * @param enable
	 */
	void enableContextMenu(boolean enable) {
		if (enable && this.measurementTypeTabItem != null) {
			this.popupMenu = new Menu(this.measurementTypeTabItem.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			//this.popupMenu = SWTResourceManager.getMenu("MeasurementContextmenu", this.measurementTypeTabItem.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			this.contextMenu = new MeasurementContextmenu(this.popupMenu, this.measurementTypeTabItem, this.measurementTypeTabItem.channelConfigMeasurementPropertiesTabFolder);
			this.contextMenu.create();
		}
		else if (this.popupMenu != null) {
			this.popupMenu.dispose();
			this.popupMenu = null;
			this.contextMenu = null;
		}
		this.propertyTypeComposite.setMenu(this.popupMenu);
		this.nameLabel.setMenu(this.popupMenu);
		this.typeLabel.setMenu(this.popupMenu);
		this.valueLabel.setMenu(this.popupMenu);
		this.descriptionLabel.setMenu(this.popupMenu);
	}

	/**
	 * method to set values of the name selection combo for cases where this names are constant
	 */
	public void setNameComboItems(String[] items) {
		this.nameCombo.setItems(items);
	}

	/**
	 * update the tab item internal widgets by property content
	 * @param useDeviceConfig of DeviceConfiguration
	 * @param useProperty of PropertyType
	 * @param enableEditName 
	 * @param nameSelectionItems String[] used to fill a selection combo box
	 * @param typeSelectionItems String[] used to fill a selection combo box
	 * @param enableEditValue
	 */
	public void setProperty(DeviceConfiguration useDeviceConfig, PropertyType useProperty, boolean enableEditName, String[] nameSelectionItems, String[] typeSelectionItems, boolean enableEditValue) {
		this.deviceConfig = useDeviceConfig;
		this.propertyType = useProperty;
		
		isStateType = enableEditName && nameSelectionItems != null && typeSelectionItems != null && !enableEditValue; 						//1000
		isValueOnlyEnabledType = !enableEditName && nameSelectionItems != null && typeSelectionItems != null && enableEditValue; 	//0001
		isNameSelectionEnabledType = !enableEditName && nameSelectionItems != null && nameSelectionItems.length > 1; 																							//01**
		isTypeSelectionEnabledType = typeSelectionItems != null && typeSelectionItems.length > 1; 																																	//**1*
	
		this.setText(this.tabName = this.propertyType.getName());

		if (isNameSelectionEnabledType) {
			this.nameText.setVisible(false);
			this.nameCombo.setVisible(true);
			this.nameCombo.setItems(nameSelectionItems);
		}
		else {
			this.nameText.setVisible(true);
			this.nameText.setEditable(enableEditName);
			this.nameCombo.setVisible(false);
		}
		
		this.typeCombo.setEnabled(isTypeSelectionEnabledType);
		
		if (PropertyTypeTabItem.this.propertyType.getType() == DataTypes.BOOLEAN) {
			this.valueText.setVisible(false);
			this.valueCombo.setVisible(true);		
			PropertyTypeTabItem.this.valueCombo.select(propertyType.getValue().equals(OSDE.STRING_TRUE) ? 0 : 1);
		}
		else {
			this.valueCombo.setVisible(false);
			this.valueText.setVisible(true);
			this.valueText.setEnabled(enableEditValue);
			this.valueText.setEditable(enableEditValue);
		}
	}

	/**
	 * find the DataTypes items and return as String[]
	 * @param property
	 * @param dataTypeItems
	 * @return
	 */
	public static String[] getDataTypesItems(PropertyType property, String[] dataTypeItems) {
		if (!MeasurementPropertyTypes.isNoneSpecified(property.getName())) {
			switch (MeasurementPropertyTypes.fromValue(property.getName())) {
			case OFFSET:
			case FACTOR:
			case REDUCTION:
			case REVOLUTION_FACTOR:
				dataTypeItems = new String[] { DataTypes.DOUBLE.value() };
				break;
			case REGRESSION_INTERVAL_SEC:
			case NUMBER_MOTOR:
			case NUMBER_CELLS:
			case PROP_N_100_W:
				dataTypeItems = new String[] { DataTypes.INTEGER.value() };
				break;
			case REGRESSION_TYPE_CURVE:
			case REGRESSION_TYPE_LINEAR:
				dataTypeItems = new String[] { DataTypes.STRING.value() };
				break;
			case IS_INVERT_CURRENT:
				dataTypeItems = new String[] { DataTypes.BOOLEAN.value() };
				break;
			}
		}
		return dataTypeItems == null ? new String[]{OSDE.STRING_EMPTY} : dataTypeItems;
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent disposeevent) {
					log.log(Level.FINEST, "statisticsTypeTabItem.widgetDisposed, event=" + disposeevent); //$NON-NLS-1$
					PropertyTypeTabItem.this.enableContextMenu(false);
				}
			});
			this.propertyTypeComposite = new Composite(this.parentTabFolder, SWT.NONE);
			this.setControl(this.propertyTypeComposite);
			this.propertyTypeComposite.setLayout(null);
			this.propertyTypeComposite.setSize(300, 160);
			this.propertyTypeComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					PropertyTypeTabItem.log.log(Level.FINEST, "this.paintControl, event=" + evt); //$NON-NLS-1$
					if (PropertyTypeTabItem.this.propertyTypeComposite.isVisible()) {
						if (PropertyTypeTabItem.this.propertyType != null) {
							if (PropertyTypeTabItem.this.nameText.isVisible()) {
								PropertyTypeTabItem.this.nameText.setText(PropertyTypeTabItem.this.propertyType.getName());
							}
							else if (PropertyTypeTabItem.this.nameCombo.isVisible()) {
								PropertyTypeTabItem.this.nameCombo.select(PropertyTypeTabItem.this.propertyType == null ? 0 : MeasurementPropertyTypes.fromValue(PropertyTypeTabItem.this.propertyType.getName())
										.ordinal());
							}

							if (PropertyTypeTabItem.this.propertyType.getType() != null) {
								PropertyTypeTabItem.this.typeCombo.select(PropertyTypeTabItem.this.propertyType.getType().ordinal());
								if (PropertyTypeTabItem.this.propertyType.getType() == DataTypes.BOOLEAN) {
									PropertyTypeTabItem.this.valueText.setVisible(false);
									PropertyTypeTabItem.this.valueCombo.setVisible(true);
									int selectionIndex = PropertyTypeTabItem.this.propertyType.getValue().equals(PropertyTypeTabItem.this.valueCombo.getItems()[0]) ? 0 : 1;
									PropertyTypeTabItem.this.valueCombo.select(selectionIndex);
								}
								else {
									PropertyTypeTabItem.this.valueText.setVisible(true);
									PropertyTypeTabItem.this.valueCombo.setVisible(false);
									PropertyTypeTabItem.this.valueText.setText(StringHelper.verifyTypedString(PropertyTypeTabItem.this.propertyType.getType(), PropertyTypeTabItem.this.propertyType.getValue()));
								}
							}
							else {
								PropertyTypeTabItem.this.typeCombo.select(0);
							}
							PropertyTypeTabItem.this.descriptionText.setText(PropertyTypeTabItem.this.propertyType.getDescription());
						}
						else {
							PropertyTypeTabItem.this.nameText.setText(OSDE.STRING_EMPTY);
							PropertyTypeTabItem.this.nameCombo.setText(OSDE.STRING_EMPTY);
							PropertyTypeTabItem.this.descriptionText.setText(OSDE.STRING_EMPTY);

						}
						PropertyTypeTabItem.this.enableContextMenu(true);
					}
				}
			});
			{
				this.nameLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.nameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0549));
				this.nameLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.nameLabel.setBounds(5, 12, 80, 20);
			}
			{
				this.typeLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.typeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0552));
				this.typeLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.typeLabel.setBounds(5, 38, 80, 20);
			}
			{
				this.valueLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.valueLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0553));
				this.valueLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.valueLabel.setBounds(5, 65, 80, 20);
			}
			{
				this.descriptionLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.descriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0554));
				this.descriptionLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.descriptionLabel.setBounds(5, 92, 80, 20);
			}
			{
				this.nameText = new Text(this.propertyTypeComposite, SWT.BORDER);
				this.nameText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.nameText.setBounds(90, 10, 200, 20);
				this.nameText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "nameText.keyReleased, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem.this.propertyType.setName(PropertyTypeTabItem.this.nameText.getText());
						if (PropertyTypeTabItem.this.deviceConfig != null) PropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
						PropertyTypeTabItem.this.setText(tabName = PropertyTypeTabItem.this.nameText.getText());
					}
				});
			}
			{
				this.nameCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.nameCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.nameCombo.setBounds(90, 10, 200, 20);
				this.nameCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "nameCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem.this.propertyType.setName(PropertyTypeTabItem.this.nameCombo.getText().toLowerCase());
					}
				});
			}
			{
				this.typeCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.typeCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.typeCombo.setItems(DataTypes.valuesAsStingArray());
				this.typeCombo.setBounds(90, 37, 120, 20);
				this.typeCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "typeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem.this.propertyType.setType(DataTypes.fromValue(PropertyTypeTabItem.this.typeCombo.getText()));
						if (PropertyTypeTabItem.this.propertyType.getType() == DataTypes.BOOLEAN) {
							PropertyTypeTabItem.this.valueText.setVisible(false);
							PropertyTypeTabItem.this.valueCombo.setVisible(true);
							PropertyTypeTabItem.this.propertyType.setValue(StringHelper.verifyTypedString(DataTypes.BOOLEAN, PropertyTypeTabItem.this.valueCombo.getText()));
							if (PropertyTypeTabItem.this.deviceConfig != null) PropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
							PropertyTypeTabItem.this.valueCombo.select(valueCombo.getText().equals(OSDE.STRING_TRUE) ? 0 : 1);
						}
						else {
							PropertyTypeTabItem.this.valueText.setVisible(true);
							PropertyTypeTabItem.this.valueCombo.setVisible(false);
							try {
								PropertyTypeTabItem.this.propertyType.setValue(StringHelper.verifyTypedString(PropertyTypeTabItem.this.propertyType.getType(), PropertyTypeTabItem.this.valueText.getText()));
							}
							catch (Exception e) {
								PropertyTypeTabItem.this.propertyType.setValue("0"); //$NON-NLS-1$
							}
							PropertyTypeTabItem.this.valueText.setText(PropertyTypeTabItem.this.propertyType.getValue());
						}
					}
				});
			}
			{
				this.valueText = new Text(this.propertyTypeComposite, SWT.BORDER);
				this.valueText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.valueText.setBounds(90, 65, 120, 20);
				this.valueText.addKeyListener(this.valueKeyListener = new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "valueText.keyReleased, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem.this.propertyType.setValue(PropertyTypeTabItem.this.valueText.getText());
						if (PropertyTypeTabItem.this.deviceConfig != null) PropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
					}
				});
				this.valueText.addVerifyListener(this.valueVerifyListener = new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "valueText.verifyText, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem.log.log(Level.FINE, evt.text);
						evt.doit = PropertyTypeTabItem.this.propertyType.getType() == null ? true : StringHelper.verifyTypedInput(PropertyTypeTabItem.this.propertyType.getType(), evt.text);
					}
				});
				this.valueCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.valueCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.valueCombo.setBounds(90, 65, 120, 20);
				this.valueCombo.setEditable(false);
				this.valueCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
				this.valueCombo.setItems(OSDE.STRING_ARRAY_TRUE_FALSE);
				this.valueCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "valueCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (PropertyTypeTabItem.this.deviceConfig != null) {
							// mode state type is Integer and can not be modified by combo selection
							
							// check type values for DesktopTypes
							String guessDesktopTypeName = PropertyTypeTabItem.this.propertyType.getName();
							if (guessDesktopTypeName.equals(DesktopPropertyTypes.TABLE_TAB.value()) || guessDesktopTypeName.equals(DesktopPropertyTypes.DIGITAL_TAB.value())
									|| guessDesktopTypeName.equals(DesktopPropertyTypes.ANALOG_TAB.value()) || guessDesktopTypeName.equals(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value())) {
								PropertyTypeTabItem.this.deviceConfig.setTableTabRequested(Boolean.parseBoolean(PropertyTypeTabItem.this.valueCombo.getText()));
							}
							else {						
								try { //check for valid and known DataType
									PropertyTypeTabItem.this.propertyType.setValue(DataTypes.fromValue(PropertyTypeTabItem.this.valueCombo.getText()).value());
								}
								catch (Exception e) {
									PropertyTypeTabItem.this.propertyType.setValue(PropertyTypeTabItem.this.valueCombo.getText());
								}
								
							}
						}
						else {
							PropertyTypeTabItem.this.propertyType.setValue(PropertyTypeTabItem.this.valueCombo.getText());
						}
						if (PropertyTypeTabItem.this.deviceConfig != null) PropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
					}
				});
			}
			{
				this.descriptionText = new Text(this.propertyTypeComposite, SWT.LEFT | SWT.WRAP | SWT.BORDER);
				this.descriptionText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.descriptionText.setBounds(90, 92, 200, 55);
				this.descriptionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "descriptionText.keyReleased, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem.this.propertyType.setDescription(PropertyTypeTabItem.this.descriptionText.getText());
						if (PropertyTypeTabItem.this.deviceConfig != null) PropertyTypeTabItem.this.deviceConfig.setChangePropery(true);
					}
				});
			}
			this.propertyTypeComposite.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return property of PropertyType
	 */
	public PropertyType getProperty() {
		return this.propertyType;
	}
}
