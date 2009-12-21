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
	final static Logger	log	= Logger.getLogger(PropertyTypeTabItem.class.getName());

	Composite						propertyTypeComposite;
	Label								nameLabel;
	Label								typeLabel;
	Label								valueLabel;
	Label								descriptionLabel;
	Text								nameText, valueText, descriptionText;
	CCombo							typeCombo, valueCombo, nameCombo;
	KeyAdapter					valueKeyListener;
	VerifyListener			valueVerifyListener;

	DeviceConfiguration	deviceConfigParent;
	boolean							isStateType; 				//1000
	boolean							isDesktopType; 			//0001
	boolean 						isMeasurementType; 	//1101
	boolean 						isUnspecifiedType; 	//1111
	PropertyType				propertyType;

	final CTabFolder		parentTabFolder;
	String							tabName;

	//	/**
	//	 * set device configuration and type parent
	//	 */
	//	public void setParents(DeviceConfiguration newDeviceConfigParent, ModeStateType newModeStateParent, DesktopType newDesktopParent) {
	//		deviceConfigParent = newDeviceConfigParent;
	//		stateParent = newModeStateParent;
	//		desktopParent = newDesktopParent;
	//	}

//	/**
//	* Auto-generated main method to display this 
//	* org.eclipse.swt.widgets.Composite inside a new Shell.
//	*/
//	public static void main(String[] args) {
//		showGUI();
//	}
//
//	/**
//	* Auto-generated method to display this 
//	* org.eclipse.swt.widgets.Composite inside a new Shell.
//	*/
//	public static void showGUI() {
//		Display display = Display.getDefault();
//		Shell shell = new Shell(display);
//		CTabFolder instTabFolder = new CTabFolder(shell, SWT.NULL);
//		new PropertyTypeTabItem(instTabFolder, SWT.NULL, "Prop 1", "offset", DataTypes.INTEGER, "25", "The offest value is added to actual measurement value");
//		instTabFolder.setSize(300, 200);
//		shell.setLayout(new FillLayout());
//		shell.layout();
//
//		shell.open();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch()) display.sleep();
//		}
//	}

	/**
	 * constructor without any variables input
	 * to display default values a call of update(PropertyType) is required
	 * @param parent
	 * @param style
	 * @param useHeader
	 */
	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName) {
		super(parent, style);
		this.parentTabFolder = parent;
		this.tabName = useTabName;
		PropertyTypeTabItem.log.log(Level.FINE, "PropertyTypeTabItem " + this.tabName);
		
		this.propertyType = new ObjectFactory().createPropertyType();
		this.propertyType.setName(Messages.getString(MessageIds.OSDE_MSGT0473));
		this.propertyType.setType(DataTypes.INTEGER);
		this.propertyType.setValue(OSDE.STRING_EMPTY + 0);
		this.propertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0474));

		initGUI();
	}

//	/**
//	 * constructor with header text and all available input parameter, 
//	 * if some parameter might not be filled use OSDE.STRING_EMPTY
//	 * @param parent
//	 * @param style
//	 * @param useHeader
//	 * @param useName
//	 * @param useType
//	 * @param useValue
//	 * @param useDescription
//	 */
//	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, String useName, DataTypes useType, Object useValue, String useDescription) {
//		super(parent, style);
//		this.tabName = useTabName;
//
//		this.parentTabFolder = parent;
//		this.propertyType = new ObjectFactory().createPropertyType();
//		this.propertyType.setName(useName);
//		this.propertyType.setType(useType);
//		this.propertyType.setValue(useValue);
//		this.propertyType.setDescription(useDescription);
//		initGUI();
//	}

//	/**
//	 * constructor with header text and all available input parameter excluding the value, 
//	 * if some parameter might not be filled use OSDE.STRING_EMPTY
//	 * @param parent
//	 * @param style
//	 * @param useHeader
//	 * @param useName
//	 * @param useType
//	 * @param useDescription
//	 */
//	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, String useName, DataTypes useType, String useDescription) {
//		super(parent, style);
//		this.parentTabFolder = parent;
//		this.tabName = useTabName;
//
//		this.propertyType = new ObjectFactory().createPropertyType();
//		this.propertyType.setName(useName);
//		this.propertyType.setType(useType);
//		this.propertyType.setDescription(useDescription);
//		initGUI();
//	}

//	/**
//	 * constructor with header text and all available input parameter from given property 
//	 * @param parent
//	 * @param style
//	 * @param useHeader
//	 * @param useProperty of PropertyType
//	 */
//	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, PropertyType useProperty) {
//		super(parent, style);
//		this.parentTabFolder = parent;
//		this.tabName = useTabName;
//
//		this.propertyType = useProperty;
//		initGUI();
//	}

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
	 * @param enableEditSelectName 
	 * @param enableSelectType 
	 * @param enableValue
	 */
	public void setProperty(DeviceConfiguration useDeviceConfig, PropertyType useProperty, boolean enableEditSelectName, boolean enableNameSelection, boolean enableSelectType, boolean enableValue) {
		this.deviceConfigParent = useDeviceConfig;
		this.propertyType = useProperty;
		
		isStateType = enableEditSelectName && !enableNameSelection && !enableSelectType && !enableValue; 				//1000
		isDesktopType = !enableEditSelectName && !enableNameSelection && !enableSelectType && enableValue; 			//0001
		isMeasurementType = enableEditSelectName && enableNameSelection && !enableSelectType && enableValue; 	//1101
		isUnspecifiedType = enableEditSelectName && enableNameSelection && enableSelectType && enableValue; 	//1111

		
		this.setText(this.tabName = this.propertyType.getName());

		if (enableNameSelection) {
			this.nameText.setVisible(false);
			this.nameCombo.setVisible(true);
		}
		else {
			this.nameText.setVisible(true);
			this.nameCombo.setVisible(false);
			this.nameText.setEditable(enableEditSelectName);
			this.nameCombo.setEditable(enableEditSelectName);
			this.nameCombo.setEnabled(enableEditSelectName);
		}
		this.typeCombo.setEnabled(enableSelectType);
		if (this.propertyType.getType() == DataTypes.BOOLEAN) {
			this.valueText.setVisible(false);
			this.valueCombo.setVisible(true);
			this.valueCombo.setEnabled(enableValue);
		}
		else {
			this.valueCombo.setVisible(false);
			this.valueText.setVisible(true);
			this.valueText.setEditable(enableValue);
		}
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			this.propertyTypeComposite = new Composite(this.parentTabFolder, SWT.NONE);
			this.setControl(this.propertyTypeComposite);
			this.propertyTypeComposite.setLayout(null);
			this.propertyTypeComposite.setSize(300, 160);
			this.propertyTypeComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					PropertyTypeTabItem.log.log(Level.FINEST, "this.paintControl, event=" + evt);
					if (PropertyTypeTabItem.this.propertyType != null) {
						if (PropertyTypeTabItem.this.nameText.isVisible()) {
							PropertyTypeTabItem.this.nameText.setText(PropertyTypeTabItem.this.propertyType.getName());
						}
						else if (PropertyTypeTabItem.this.nameCombo.isVisible()) {
							PropertyTypeTabItem.this.nameCombo.select(
									PropertyTypeTabItem.this.propertyType == null ? 0 : MeasurementPropertyTypes.fromValue(PropertyTypeTabItem.this.propertyType.getName())
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
				}
			});
			{
				this.nameLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.nameLabel.setText("name");
				this.nameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.nameLabel.setBounds(5, 12, 80, 20);
			}
			{
				this.typeLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.typeLabel.setText("type");
				this.typeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.typeLabel.setBounds(5, 38, 80, 20);
			}
			{
				this.valueLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.valueLabel.setText("value");
				this.valueLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.valueLabel.setBounds(5, 65, 80, 20);
			}
			{
				this.descriptionLabel = new Label(this.propertyTypeComposite, SWT.RIGHT);
				this.descriptionLabel.setText("description");
				this.descriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.descriptionLabel.setBounds(5, 92, 80, 20);
			}
			{
				this.nameText = new Text(this.propertyTypeComposite, SWT.BORDER);
				this.nameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.nameText.setBounds(90, 10, 200, 20);
				this.nameText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "nameText.keyReleased, event=" + evt);
						if (PropertyTypeTabItem.this.deviceConfigParent != null) {
							if (isStateType) {
								PropertyTypeTabItem.this.deviceConfigParent.setStateName(Integer.parseInt(PropertyTypeTabItem.this.propertyType.getValue()), PropertyTypeTabItem.this.nameText.getText());
								//DesktopType name is key and can not be modified
								//MeasurementTypeProperty name is key and can be selected factor, offset, reduction
							}
						}
						else {
							PropertyTypeTabItem.this.propertyType.setName(PropertyTypeTabItem.this.nameText.getText());
						}
						PropertyTypeTabItem.this.setText(tabName = PropertyTypeTabItem.this.nameText.getText());
					}
				});
			}
			{
				this.nameCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.nameCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.nameCombo.setBounds(90, 10, 200, 20);
				//nameCombo.setEditable(false);
				//nameCombo.setEnabled(false);
				this.nameCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "nameCombo.widgetSelected, event=" + evt);
						if (PropertyTypeTabItem.this.deviceConfigParent != null) {
							if (isMeasurementType)
								PropertyTypeTabItem.this.deviceConfigParent.setMeasurementName("test", Integer.parseInt(PropertyTypeTabItem.this.propertyType.getValue()), PropertyTypeTabItem.this.nameCombo.getText());
							//TODO 
							//desktop name is key and can be modified
						}
						else
							PropertyTypeTabItem.this.propertyType.setName(PropertyTypeTabItem.this.nameCombo.getText().toLowerCase());
					}
				});
			}
			{
				this.typeCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.typeCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.typeCombo.setItems(DataTypes.valuesAsStingArray());
				this.typeCombo.setBounds(90, 37, 120, 20);
				this.typeCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "typeCombo.widgetSelected, event=" + evt);
						PropertyTypeTabItem.this.propertyType.setType(DataTypes.valueOf(PropertyTypeTabItem.this.typeCombo.getText()));
						if (PropertyTypeTabItem.this.propertyType.getType() == DataTypes.BOOLEAN) {
							PropertyTypeTabItem.this.valueText.setVisible(false);
							PropertyTypeTabItem.this.valueCombo.setVisible(true);
							PropertyTypeTabItem.this.propertyType.setValue(StringHelper.verifyTypedString(DataTypes.BOOLEAN, PropertyTypeTabItem.this.valueCombo.getText()));
							if (PropertyTypeTabItem.this.deviceConfigParent != null) {
								// mode state type is Integer and can not be modified
								// desktop state type is Boolean and can be modified
							}
							else
								PropertyTypeTabItem.this.valueCombo.select(evt.text.equals("true") ? 0 : 1);
						}
						else {
							PropertyTypeTabItem.this.valueText.setVisible(true);
							PropertyTypeTabItem.this.valueCombo.setVisible(false);
							try {
								if (PropertyTypeTabItem.this.deviceConfigParent != null) {
									// mode state type is Integer and can not be modified
									// desktop state type is Boolean and can be modified
								}
								else
									PropertyTypeTabItem.this.propertyType.setValue(StringHelper.verifyTypedString(PropertyTypeTabItem.this.propertyType.getType(), PropertyTypeTabItem.this.valueText.getText()));
							}
							catch (Exception e) {
								PropertyTypeTabItem.this.propertyType.setValue("0");
							}
							PropertyTypeTabItem.this.valueText.setText(PropertyTypeTabItem.this.propertyType.getValue());
						}
					}
				});
			}
			{
				this.valueText = new Text(this.propertyTypeComposite, SWT.BORDER);
				this.valueText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.valueText.setBounds(90, 65, 120, 20);
				this.valueText.addKeyListener(this.valueKeyListener = new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "valueText.keyReleased, event=" + evt);
						if (PropertyTypeTabItem.this.deviceConfigParent != null) {
							//TODO
							// mode state type is Integer and can not be modified
							// desktop state type is Boolean and can be modified as text
						}
						else
							PropertyTypeTabItem.this.propertyType.setValue(PropertyTypeTabItem.this.valueText.getText());
					}
				});
				this.valueText.addVerifyListener(this.valueVerifyListener = new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "valueText.verifyText, event=" + evt);
						PropertyTypeTabItem.log.log(Level.FINE, evt.text);
						evt.doit = PropertyTypeTabItem.this.propertyType.getType() == null ? true : StringHelper.verifyTypedInput(PropertyTypeTabItem.this.propertyType.getType(), evt.text);
					}
				});
				this.valueCombo = new CCombo(this.propertyTypeComposite, SWT.BORDER);
				this.valueCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.valueCombo.setBounds(90, 65, 120, 20);
				this.valueCombo.setEditable(false);
				this.valueCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
				this.valueCombo.setItems(new String[] { "true", "false" });
				this.valueCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "valueCombo.widgetSelected, event=" + evt);
						if (PropertyTypeTabItem.this.deviceConfigParent != null) {
							// mode state type is Integer and can not be modified by combo selection
							if (isDesktopType) {
								if (PropertyTypeTabItem.this.propertyType.getName().equals(DesktopPropertyTypes.TABLE_TAB.value()))
									PropertyTypeTabItem.this.deviceConfigParent.setTableTabRequested(Boolean.parseBoolean(PropertyTypeTabItem.this.valueCombo.getText()));
								else if (PropertyTypeTabItem.this.propertyType.getName().equals(DesktopPropertyTypes.DIGITAL_TAB.value()))
									PropertyTypeTabItem.this.deviceConfigParent.setDigitalTabRequested(Boolean.parseBoolean(PropertyTypeTabItem.this.valueCombo.getText()));
								else if (PropertyTypeTabItem.this.propertyType.getName().equals(DesktopPropertyTypes.ANALOG_TAB.value()))
									PropertyTypeTabItem.this.deviceConfigParent.setAnalogTabRequested(Boolean.parseBoolean(PropertyTypeTabItem.this.valueCombo.getText()));
								else if (PropertyTypeTabItem.this.propertyType.getName().equals(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value()))
									PropertyTypeTabItem.this.deviceConfigParent.setVoltagePerCellTabRequested(Boolean.parseBoolean(PropertyTypeTabItem.this.valueCombo.getText()));
							}
						}
						else
							PropertyTypeTabItem.this.propertyType.setValue(PropertyTypeTabItem.this.valueCombo.getText());
					}
				});
			}
			{
				this.descriptionText = new Text(this.propertyTypeComposite, SWT.LEFT | SWT.WRAP | SWT.BORDER);
				this.descriptionText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.descriptionText.setBounds(90, 92, 200, 55);
				this.descriptionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						PropertyTypeTabItem.log.log(Level.FINEST, "descriptionText.keyReleased, event=" + evt);
						if (PropertyTypeTabItem.this.deviceConfigParent != null) {
							if (isStateType)
								PropertyTypeTabItem.this.deviceConfigParent.setStateDescription(Integer.parseInt(PropertyTypeTabItem.this.propertyType.getValue()), PropertyTypeTabItem.this.descriptionText.getText());
							else if (isDesktopType)
								PropertyTypeTabItem.this.deviceConfigParent.setDesktopTypeDesription(DesktopPropertyTypes.fromValue(PropertyTypeTabItem.this.propertyType.getName()), PropertyTypeTabItem.this.descriptionText.getText());
							else if (isMeasurementType)
								PropertyTypeTabItem.this.deviceConfigParent.setDesktopTypeDesription(DesktopPropertyTypes.fromValue(PropertyTypeTabItem.this.propertyType.getName()), PropertyTypeTabItem.this.descriptionText.getText());
							else //isUnspecifiedType TODO
								PropertyTypeTabItem.this.deviceConfigParent.setDesktopTypeDesription(DesktopPropertyTypes.fromValue(PropertyTypeTabItem.this.propertyType.getName()), PropertyTypeTabItem.this.descriptionText.getText());
						}
						else
							PropertyTypeTabItem.this.propertyType.setDescription(PropertyTypeTabItem.this.descriptionText.getText());
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
