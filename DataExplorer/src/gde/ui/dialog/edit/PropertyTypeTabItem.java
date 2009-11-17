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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.DesktopType;
import osde.device.DeviceConfiguration;
import osde.device.MeasurementPropertyTypes;
import osde.device.StateType;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * Composite to wrap XML PropertyType enable to edit existing and create new device property files 
 * @author Winfried Brügmann
 */
public class PropertyTypeTabItem extends CTabItem {
	final static Logger						log			= Logger.getLogger(PropertyTypeTabItem.class.getName());

	Composite propertyTypeComposite;
	Label nameLabel;
	Label typeLabel;
	Label valueLabel;
	Label descriptionLabel;
	Text nameText, valueText, descriptionText;
	CCombo typeCombo, valueCombo, nameCombo;
	KeyAdapter valueKeyListener;
	VerifyListener valueVerifyListener;
	
	DeviceConfiguration deviceConfigParent;
	StateType stateParent;
	DesktopType desktopParent;
	PropertyType propertyType;
	
	final CTabFolder parentTabFolder;
	String tabName;
	
//	/**
//	 * set device configuration and type parent
//	 */
//	public void setParents(DeviceConfiguration newDeviceConfigParent, ModeStateType newModeStateParent, DesktopType newDesktopParent) {
//		deviceConfigParent = newDeviceConfigParent;
//		stateParent = newModeStateParent;
//		desktopParent = newDesktopParent;
//	}
	
	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void main(String[] args) {
		showGUI();
	}
		
	/**
	* Auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		CTabFolder instTabFolder = new CTabFolder(shell, SWT.NULL);
		new PropertyTypeTabItem(instTabFolder, SWT.NULL, "Prop 1", "offset", DataTypes.INTEGER, "25", "The offest value is added to actual measurement value");
		instTabFolder.setSize(300, 200);
		shell.setLayout(new FillLayout());
		shell.layout();
		
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * constructor without any variables input
	 * to display default values a call of update(PropertyType) is required
	 * @param parent
	 * @param style
	 * @param useHeader
	 */
	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName) {
		super(parent, style);
		parentTabFolder = parent;
		tabName = useTabName;
		System.out.println("PropertyTypeTabItem " + tabName);
		initGUI();
	}

	/**
	 * constructor with header text and all available input parameter, 
	 * if some parameter might not be filled use OSDE.STRING_EMPTY
	 * @param parent
	 * @param style
	 * @param useHeader
	 * @param useName
	 * @param useType
	 * @param useValue
	 * @param useDescription
	 */
	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, String useName, DataTypes useType, Object useValue, String useDescription) {
		super(parent, style);
		tabName = useTabName;

		parentTabFolder = parent;
		propertyType = new ObjectFactory().createPropertyType();
		propertyType.setName(useName);
		propertyType.setType(useType);
		propertyType.setValue(useValue);
		propertyType.setDescription(useDescription);
		initGUI();
	}
	
	/**
	 * constructor with header text and all available input parameter excluding the value, 
	 * if some parameter might not be filled use OSDE.STRING_EMPTY
	 * @param parent
	 * @param style
	 * @param useHeader
	 * @param useName
	 * @param useType
	 * @param useDescription
	 */
	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, String useName, DataTypes useType, String useDescription) {
		super(parent, style);
		parentTabFolder = parent;
		tabName = useTabName;

		propertyType = new ObjectFactory().createPropertyType();
		propertyType.setName(useName);
		propertyType.setType(useType);
		propertyType.setDescription(useDescription);
		initGUI();
	}
	
	/**
	 * constructor with header text and all available input parameter from given property 
	 * @param parent
	 * @param style
	 * @param useHeader
	 * @param useProperty of PropertyType
	 */
	public PropertyTypeTabItem(CTabFolder parent, int style, String useTabName, PropertyType useProperty) {
		super(parent, style);
		parentTabFolder = parent;
		tabName = useTabName;

		this.propertyType = useProperty;
		initGUI();
	}
	
	public void setNameComboItems(String[] items) {
		nameCombo.setItems(items);
	}
	
	/**
	 * update the tab item internal widgets by property content
	 * @param useProperty of PropertyType
	 * @param enableEditSelectName 
	 * @param enableSelectType 
	 * @param enableValue
	 */
	public void setProperty(PropertyType useProperty, boolean enableEditSelectName, boolean enableNameSelection, boolean enableSelectType, boolean enableValue) {
		propertyType = useProperty;
		if (enableNameSelection) {
			nameText.setVisible(false);
			nameCombo.setVisible(true);
		}
		else {
			nameText.setVisible(true);
			nameCombo.setVisible(false);
			nameText.setEditable(enableEditSelectName);
			nameCombo.setEditable(enableEditSelectName);
			nameCombo.setEnabled(enableEditSelectName);
		}
		typeCombo.setEnabled(enableSelectType);
		if (propertyType.getType() == DataTypes.BOOLEAN) {
			valueText.setVisible(false);
			valueCombo.setVisible(true);
			valueCombo.setEnabled(enableValue);
		}
		else {
			valueCombo.setVisible(false);
			valueText.setVisible(true);
			valueText.setEditable(enableValue);
		}
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			propertyTypeComposite = new Composite(parentTabFolder, SWT.NONE);
			this.setControl(propertyTypeComposite);
			propertyTypeComposite.setLayout(null);
			propertyTypeComposite.setSize(300, 160);
			propertyTypeComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "this.paintControl, event="+evt);
					if (propertyType != null) PropertyTypeTabItem.this.setText(tabName = propertyType.getName());
					if (nameText.isVisible()) {
						if (propertyType == null) {
							nameText.setText(OSDE.STRING_EMPTY);
						}
						else {
							nameText.setText(propertyType.getName());
						}
					}
					if (nameCombo.isVisible()) {
						nameCombo.select(propertyType == null ? 0 : MeasurementPropertyTypes.fromValue(propertyType.getName()).ordinal());
					}
					if (propertyType != null) {
						if (propertyType.getType() != null) {
							typeCombo.select(propertyType.getType().ordinal());
							if (propertyType.getType() == DataTypes.BOOLEAN) {
								valueText.setVisible(false);
								valueCombo.setVisible(true);
								int selectionIndex = propertyType.getValue().equals(valueCombo.getItems()[0]) ? 0 : 1;
								valueCombo.select(selectionIndex);
							}
							else {
								valueText.setVisible(true);
								valueCombo.setVisible(false);
								valueText.setText(StringHelper.verifyTypedString(propertyType.getType(), propertyType.getValue()));
							}
						}
						else {
							typeCombo.select(0);
						}
					}
					
					if (propertyType != null) descriptionText.setText(propertyType.getDescription());
				}
			});
			{
				nameLabel = new Label(propertyTypeComposite, SWT.RIGHT);
				nameLabel.setText("name");
				nameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				nameLabel.setBounds(5, 12, 80, 20);
			}
			{
				typeLabel = new Label(propertyTypeComposite, SWT.RIGHT);
				typeLabel.setText("type");
				typeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				typeLabel.setBounds(5, 38, 80, 20);
			}
			{
				valueLabel = new Label(propertyTypeComposite, SWT.RIGHT);
				valueLabel.setText("value");
				valueLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				valueLabel.setBounds(5, 65, 80, 20);
			}
			{
				descriptionLabel = new Label(propertyTypeComposite, SWT.RIGHT);
				descriptionLabel.setText("description");
				descriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				descriptionLabel.setBounds(5, 92, 80, 20);
			}
			{
				nameText = new Text(propertyTypeComposite, SWT.BORDER);
				nameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				nameText.setBounds(90, 10, 200, 20);
				nameText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "nameText.keyReleased, event="+evt);
						if (deviceConfigParent != null) {
							if (stateParent != null) deviceConfigParent.setStateName(Integer.parseInt(propertyType.getValue()), nameText.getText());
							//desktop name is key and can be modified
						}
						else  propertyType.setName(nameText.getText());
					}
				});
			}
			{
				nameCombo = new CCombo(propertyTypeComposite, SWT.BORDER);
				nameCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				nameCombo.setBounds(90, 10, 200, 20);
				//nameCombo.setEditable(false);
				//nameCombo.setEnabled(false);
				nameCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "nameCombo.widgetSelected, event="+evt);
						if (deviceConfigParent != null) {
							if (stateParent != null) deviceConfigParent.setStateName(Integer.parseInt(propertyType.getValue()), nameText.getText());
							//desktop name is key and can be modified
						}
						else  propertyType.setName(nameText.getText());
					}
				});
			}
			{
				typeCombo = new CCombo(propertyTypeComposite, SWT.BORDER);
				typeCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				typeCombo.setItems(DataTypes.valuesAsStingArray());
				typeCombo.setBounds(90, 37, 120, 20);
				typeCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "typeCombo.widgetSelected, event="+evt);
						propertyType.setType(DataTypes.valueOf(typeCombo.getText()));
						if (propertyType.getType() == DataTypes.BOOLEAN) {
							valueText.setVisible(false);
							valueCombo.setVisible(true);
							propertyType.setValue(StringHelper.verifyTypedString(DataTypes.BOOLEAN, valueCombo.getText()));
							if (deviceConfigParent != null) {
								// mode state type is Integer and can not be modified
								// desktop state type is Boolean and can be modified
							}
							else valueCombo.select(evt.text.equals("true")?0:1);
						}
						else {
							valueText.setVisible(true);
							valueCombo.setVisible(false);
							try {
								if (deviceConfigParent != null) {
									// mode state type is Integer and can not be modified
									// desktop state type is Boolean and can be modified
								}
								else propertyType.setValue(StringHelper.verifyTypedString(propertyType.getType(), valueText.getText()));
							}
							catch (Exception e) {
								propertyType.setValue("0");
							}
							valueText.setText(propertyType.getValue());
						}
					}
				});
			}
			{
				valueText = new Text(propertyTypeComposite, SWT.BORDER);
				valueText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				valueText.setBounds(90, 65, 120, 20);
				valueText.addKeyListener(valueKeyListener = new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "valueText.keyReleased, event="+evt);
						if (deviceConfigParent != null) {
							// mode state type is Integer and can not be modified
							// desktop state type is Boolean and can be modified as text
						}
						else propertyType.setValue(valueText.getText());
					}
				});
				valueText.addVerifyListener(valueVerifyListener = new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						log.log(Level.FINEST, "valueText.verifyText, event="+evt);
						log.log(Level.FINE, evt.text);
						evt.doit = propertyType.getType() == null ? true : StringHelper.verifyTypedInput(propertyType.getType(), evt.text);
					}
				});
				valueCombo = new CCombo(propertyTypeComposite, SWT.BORDER);
				valueCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				valueCombo.setBounds(90, 65, 120, 20);
				valueCombo.setEditable(false);
				valueCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
				valueCombo.setItems(new String[] {"true", "false"});
				valueCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "valueCombo.widgetSelected, event="+evt);
						if (deviceConfigParent != null) {
							// mode state type is Integer and can not be modified by combo selection
							if (desktopParent != null) {
								if (propertyType.getName().equals(DesktopType.TYPE_TABLE_TAB)) 
									deviceConfigParent.setTableTabRequested(Boolean.parseBoolean(valueCombo.getText()));
								else if (propertyType.getName().equals(DesktopType.TYPE_DIGITAL_TAB)) 
									deviceConfigParent.setDigitalTabRequested(Boolean.parseBoolean(valueCombo.getText()));
								else if (propertyType.getName().equals(DesktopType.TYPE_ANALOG_TAB)) 
									deviceConfigParent.setAnalogTabRequested(Boolean.parseBoolean(valueCombo.getText()));
								else if (propertyType.getName().equals(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB)) 
									deviceConfigParent.setVoltagePerCellTabRequested(Boolean.parseBoolean(valueCombo.getText()));
							}
						}
						else propertyType.setValue(valueCombo.getText());
					}
				});
			}
			{
				descriptionText = new Text(propertyTypeComposite, SWT.LEFT | SWT.WRAP | SWT.BORDER);
				descriptionText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				descriptionText.setBounds(90, 92, 200, 55);
				descriptionText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "descriptionText.keyReleased, event="+evt);
						if (deviceConfigParent != null) {
							if (stateParent != null) deviceConfigParent.setStateDescription(Integer.parseInt(propertyType.getValue()), descriptionText.getText());
							else if (desktopParent != null) deviceConfigParent.setDesktopTypeDesription(propertyType.getName(), descriptionText.getText());
						}
						else propertyType.setDescription(descriptionText.getText());
					}
				});
			}
			propertyTypeComposite.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * @return property of PropertyType
	 */
	public PropertyType getProperty() {
		return propertyType;
	}
}
