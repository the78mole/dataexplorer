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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.DeviceConfiguration;
import osde.device.MeasurementPropertyTypes;
import osde.device.MeasurementType;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.device.StatisticsType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.SWTResourceManager;

/**
 * class defining a CTabItem with MeasurementType configuration data
 * @author Winfried Brügmann
 */
public class MeasurementTypeTabItem extends CTabItem {
	final static Logger						log	= Logger.getLogger(MeasurementTypeTabItem.class.getName());

	final CTabFolder							measurementsTabFolder;
	String												tabName;

	Composite											measurementsComposite;
	Label													measurementNameLabel, measurementSymbolLabel, measurementUnitLabel, measurementEnableLabel;
	Text													measurementNameText, measurementSymbolText, measurementUnitText;
	Button												measurementActiveButton;
	CTabFolder										channelConfigMeasurementPropertiesTabFolder;
	Button												addMeasurementButton;
	Label													measurementTypeLabel;
	Menu													popupMenu;
	MeasurementContextmenu				contextMenu;

	String												measurementName, measurementSymbol, measurementUnit;
	boolean												isMeasurementActive;

	CTabFolder										measurementPropertiesTabFolder;
	CTabItem											measurementPropertiesTabItem;
	StatisticsTypeTabItem					statisticsTypeTabItem;

	DeviceConfiguration						deviceConfig;
	int														channelConfigNumber;
	MeasurementType								measurementType;

	final DevicePropertiesEditor	propsEditor;

	public MeasurementTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		this.measurementsTabFolder = parent;
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		initGUI();
	}

	@Override
	public synchronized MeasurementTypeTabItem clone() {
		return new MeasurementTypeTabItem(this);
	}

	/**
	 * copy constructor
	 * @param copyFrom
	 */
	private MeasurementTypeTabItem(MeasurementTypeTabItem copyFrom) {
		super(copyFrom.measurementsTabFolder, SWT.CLOSE);
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.measurementsTabFolder = copyFrom.measurementsTabFolder;
		this.measurementName = Messages.getString(MessageIds.OSDE_MSGT0529);
		this.measurementSymbol = copyFrom.measurementSymbol;
		this.measurementUnit = copyFrom.measurementUnit;
		this.isMeasurementActive = copyFrom.isMeasurementActive;

		this.deviceConfig = copyFrom.deviceConfig;
		this.channelConfigNumber = copyFrom.channelConfigNumber;
		this.tabName = OSDE.STRING_BLANK + (this.deviceConfig != null ? this.measurementName : (this.measurementsTabFolder.getItemCount())) + OSDE.STRING_BLANK;

		initGUI();

		if (this.deviceConfig != null) {
			this.measurementType = copyFrom.measurementType.clone(); // this will clone statistics and properties as well
			this.measurementType.setName(this.measurementName);
			this.deviceConfig.addMeasurement2Channel(this.channelConfigNumber, this.measurementType);

			//update statistics
			if (this.statisticsTypeTabItem != null) {
				StatisticsType tmpStatisticsType = this.measurementType.getStatistics().clone();
				tmpStatisticsType.removeTrigger();
				this.statisticsTypeTabItem.setStatisticsType(this.deviceConfig, tmpStatisticsType, this.channelConfigNumber);
			}

			// update properties
			int propertyCount = this.measurementPropertiesTabFolder != null ? this.measurementPropertiesTabFolder.getItemCount() : 0;
			int measurementPropertyCount = this.measurementType.getProperty().size();
			if (measurementPropertyCount > 0 && (this.measurementPropertiesTabItem == null || this.measurementPropertiesTabItem.isDisposed())) { // there are measurement properties, but no properties tab folder
				this.measurementPropertiesTabItem = new CTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
				this.measurementPropertiesTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0530));
				this.measurementPropertiesTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.measurementPropertiesTabFolder = new CTabFolder(this.channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
				this.measurementPropertiesTabItem.setControl(this.measurementPropertiesTabFolder);
			}
			if (propertyCount < measurementPropertyCount) {
				for (int i = propertyCount; i < measurementPropertyCount; i++) {
					new PropertyTypeTabItem(this.measurementPropertiesTabFolder, SWT.CLOSE, OSDE.STRING_STAR, this);
				}
			}
			else if (propertyCount > measurementPropertyCount) {
				for (int i = measurementPropertyCount; i < propertyCount; i++) {
					this.measurementPropertiesTabFolder.getItem(0).dispose();
				}
			}
			for (int i = 0; i < measurementPropertyCount; i++) {
				PropertyTypeTabItem tabItem = (PropertyTypeTabItem) this.measurementPropertiesTabFolder.getItem(i);
				boolean isNoneSpecified = MeasurementPropertyTypes.isNoneSpecified(this.measurementType.getProperty().get(i).getName());
				tabItem.setProperty(this.deviceConfig, this.measurementType.getProperty().get(i), isNoneSpecified, isNoneSpecified ? MeasurementPropertyTypes.valuesAsStingArray() : null,
						isNoneSpecified ? DataTypes.valuesAsStingArray() : null, true);
				tabItem.setNameComboItems(MeasurementPropertyTypes.valuesAsStingArray());
			}
			if (measurementPropertyCount == 0) { // no measurement properties -> remove Properties tab folder
				if (this.measurementPropertiesTabFolder != null) {
					this.measurementPropertiesTabFolder.dispose();
					this.measurementPropertiesTabFolder = null;
					if (this.measurementPropertiesTabItem != null) {
						this.measurementPropertiesTabItem.dispose();
						this.measurementPropertiesTabItem = null;
					}
				}
			}
			else {
				this.measurementPropertiesTabFolder.setSelection(0);
			}
		}
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

		this.setText(OSDE.STRING_BLANK + this.measurementName + OSDE.STRING_BLANK);
		this.measurementsComposite.redraw();

		//begin statistics
		if (this.measurementType.getStatistics() == null && this.statisticsTypeTabItem != null && !this.statisticsTypeTabItem.isDisposed()) {
			this.statisticsTypeTabItem.dispose();
		}
		else if (this.measurementType.getStatistics() != null && (this.statisticsTypeTabItem == null || this.statisticsTypeTabItem.isDisposed())) {
			this.statisticsTypeTabItem = createStatisticsTabItem();
		}
		if (this.measurementType.getStatistics() != null && this.statisticsTypeTabItem != null && !this.statisticsTypeTabItem.isDisposed()) {
			this.statisticsTypeTabItem.setStatisticsType(this.deviceConfig, this.measurementType.getStatistics(), this.channelConfigNumber);
		}
		//end statistics

		//begin properties
		int propertyCount = this.measurementPropertiesTabFolder != null ? this.measurementPropertiesTabFolder.getItemCount() : 0;
		int measurementPropertyCount = this.measurementType.getProperty().size();
		if (measurementPropertyCount > 0 && (this.measurementPropertiesTabItem == null || this.measurementPropertiesTabItem.isDisposed())) { // there are measurement properties, but no properties tab folder
			this.measurementPropertiesTabItem = new CTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
			this.measurementPropertiesTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0530));
			this.measurementPropertiesTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.measurementPropertiesTabFolder = new CTabFolder(this.channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
			this.measurementPropertiesTabItem.setControl(this.measurementPropertiesTabFolder);
		}
		else if (measurementPropertyCount == 0) {
			if (this.measurementPropertiesTabFolder != null) { // dispose PropertyTypes
				for (CTabItem tmpPropertyTypeTabItem : this.measurementPropertiesTabFolder.getItems()) {
					((PropertyTypeTabItem) tmpPropertyTypeTabItem).dispose();
				}
				this.measurementPropertiesTabItem.dispose();
			}
		}
		if (propertyCount < measurementPropertyCount) {
			if (this.measurementPropertiesTabFolder == null || this.measurementPropertiesTabFolder.isDisposed()) {
				this.createMeasurementPropertyTabItemWithSubTabFolder();
			}
			for (int i = propertyCount; i < measurementPropertyCount; i++) {
				new PropertyTypeTabItem(this.measurementPropertiesTabFolder, SWT.CLOSE, OSDE.STRING_EMPTY, this);
			}
		}
		else if (propertyCount > measurementPropertyCount && measurementPropertyCount > 0) {
			for (int i = measurementPropertyCount; i < propertyCount; i++) {
				((PropertyTypeTabItem) this.measurementPropertiesTabFolder.getItem(i - 1)).dispose();
			}
		}
		for (int i = 0; i < measurementPropertyCount; i++) {
			PropertyTypeTabItem tabItem = (PropertyTypeTabItem) this.measurementPropertiesTabFolder.getItem(i);
			boolean isNoneSpecified = MeasurementPropertyTypes.isNoneSpecified(this.measurementType.getProperty().get(i).getName());
			tabItem.setProperty(this.deviceConfig, this.measurementType.getProperty().get(i), isNoneSpecified, isNoneSpecified ? MeasurementPropertyTypes.valuesAsStingArray() : null,
					isNoneSpecified ? DataTypes.valuesAsStingArray() : null, true);
			tabItem.setNameComboItems(MeasurementPropertyTypes.valuesAsStingArray());
		}
		if (measurementPropertyCount == 0) { // no measurement properties -> remove Properties tab folder
			if (this.measurementPropertiesTabFolder != null) {
				if (this.measurementPropertiesTabItem != null) {
					this.measurementPropertiesTabItem.dispose();
					this.measurementPropertiesTabItem = null;
				}
				this.measurementPropertiesTabFolder.dispose();
				this.measurementPropertiesTabFolder = null;
			}
		}
		else {
			this.measurementPropertiesTabFolder.setSelection(0);
		}
		//end properties
	}

	public MeasurementTypeTabItem(CTabFolder parent, int style, int index, MeasurementType useMeasurementType) {
		super(parent, style, index);
		this.measurementsTabFolder = parent;
		this.propsEditor = DevicePropertiesEditor.getInstance();
		this.tabName = OSDE.STRING_BLANK + (index + 1) + OSDE.STRING_BLANK;
		this.measurementType = useMeasurementType;
		initGUI();
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(java.util.logging.Level.FINEST, "measurementtypeTabItem.widgetDisposed, event=" + evt); //$NON-NLS-1$
					MeasurementTypeTabItem.this.enableContextMenu(false);
				}
			});
			{
				this.measurementsComposite = new Composite(this.measurementsTabFolder, SWT.NONE);
				this.measurementsComposite.setLayout(null);
				this.setControl(this.measurementsComposite);
				this.measurementsComposite.addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent evt) {
						log.log(java.util.logging.Level.FINEST, "channelConfigComposite.paintControl, event=" + evt); //$NON-NLS-1$
						if (MeasurementTypeTabItem.this.measurementsComposite.isVisible()) {
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementNameText.setText(MeasurementTypeTabItem.this.measurementName);
								MeasurementTypeTabItem.this.measurementSymbolText.setText(MeasurementTypeTabItem.this.measurementSymbol);
								MeasurementTypeTabItem.this.measurementUnitText.setText(MeasurementTypeTabItem.this.measurementUnit);
								MeasurementTypeTabItem.this.measurementActiveButton.setSelection(MeasurementTypeTabItem.this.isMeasurementActive);
							}
						}
					}
				});
				{
					this.measurementTypeLabel = new Label(this.measurementsComposite, SWT.NONE);
					this.measurementTypeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0542));
					this.measurementTypeLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementTypeLabel.setBounds(10, 10, 120, 20);
				}
				{
					this.addMeasurementButton = new Button(this.measurementsComposite, SWT.PUSH | SWT.CENTER);
					this.addMeasurementButton.setText(OSDE.STRING_PLUS);
					this.addMeasurementButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0548));
					this.addMeasurementButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.addMeasurementButton.setBounds(180, 10, 40, 20);
					this.addMeasurementButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addMeasurementButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem.this.measurementsTabFolder.getItem(MeasurementTypeTabItem.this.measurementsTabFolder.getItemCount() - 1).setShowClose(false);
							MeasurementTypeTabItem.this.measurementsTabFolder.setSelection(MeasurementTypeTabItem.this.clone());
						}
					});
				}
				{
					this.measurementNameLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0549));
					this.measurementNameLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementNameLabel.setBounds(10, 40, 60, 20);
				}
				{
					this.measurementNameText = new Text(this.measurementsComposite, SWT.BORDER);
					this.measurementNameText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementNameText.setBounds(80, 40, 145, 20);
					this.measurementNameText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementNameText.keyReleased, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem.this.measurementName = MeasurementTypeTabItem.this.measurementNameText.getText();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setName(MeasurementTypeTabItem.this.measurementName);
							}
							MeasurementTypeTabItem.this.setText(MeasurementTypeTabItem.this.tabName = OSDE.STRING_BLANK + MeasurementTypeTabItem.this.measurementName + OSDE.STRING_BLANK);
						}
					});
				}
				{
					this.measurementSymbolLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementSymbolLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0550));
					this.measurementSymbolLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementSymbolLabel.setBounds(10, 65, 60, 20);
				}
				{
					this.measurementSymbolText = new Text(this.measurementsComposite, SWT.BORDER);
					this.measurementSymbolText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementSymbolText.setBounds(80, 65, 40, 20);
					this.measurementSymbolText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementSymbolText.keyReleased, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem.this.measurementSymbol = MeasurementTypeTabItem.this.measurementSymbolText.getText();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setSymbol(MeasurementTypeTabItem.this.measurementSymbol);
							}
						}
					});
				}
				{
					this.measurementUnitLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementUnitLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0551));
					this.measurementUnitLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementUnitLabel.setBounds(10, 90, 60, 20);
				}
				{
					this.measurementUnitText = new Text(this.measurementsComposite, SWT.BORDER);
					this.measurementUnitText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementUnitText.setBounds(80, 90, 40, 20);
					this.measurementUnitText.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementUnitText.keyReleased, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem.this.measurementUnit = MeasurementTypeTabItem.this.measurementUnitText.getText();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setUnit(MeasurementTypeTabItem.this.measurementUnit);
							}
						}
					});
				}
				{
					this.measurementEnableLabel = new Label(this.measurementsComposite, SWT.RIGHT);
					this.measurementEnableLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0531));
					this.measurementEnableLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0532));
					this.measurementEnableLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementEnableLabel.setBounds(3, 115, 67, 20);
				}
				{
					this.measurementActiveButton = new Button(this.measurementsComposite, SWT.CHECK);
					this.measurementActiveButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurementActiveButton.setBounds(80, 115, 20, 20);
					this.measurementActiveButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "measurementEnableButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							MeasurementTypeTabItem.this.isMeasurementActive = MeasurementTypeTabItem.this.measurementActiveButton.getSelection();
							if (MeasurementTypeTabItem.this.measurementType != null) {
								MeasurementTypeTabItem.this.measurementType.setActive(MeasurementTypeTabItem.this.isMeasurementActive);
							}
						}
					});
				}
				{
					this.channelConfigMeasurementPropertiesTabFolder = new CTabFolder(this.measurementsComposite, SWT.BORDER);
					this.channelConfigMeasurementPropertiesTabFolder.setBounds(237, 0, 379 + 45, 199 + 30 + 15);
					//this.channelConfigMeasurementPropertiesTabFolder.setMenu(this.popupMenu);
					{
						createStatisticsTabItem();
					}
					{
						createMeasurementPropertyTabItemWithSubTabFolder();
						{
							createMeasurementPropertyTabItem(MeasurementPropertyTypes.OFFSET.value());
							createMeasurementPropertyTabItem(MeasurementPropertyTypes.FACTOR.value());
							createMeasurementPropertyTabItem(MeasurementPropertyTypes.REDUCTION.value());
						}
						this.measurementPropertiesTabFolder.setSelection(0);
						this.measurementPropertiesTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
							@Override
							public void restore(CTabFolderEvent evt) {
								log.log(java.util.logging.Level.FINE, "measurementsPropertiesTabFolder.restore, event=" + evt); //$NON-NLS-1$
								((CTabItem) evt.item).getControl();
							}

							@Override
							public void close(CTabFolderEvent evt) {
								log.log(java.util.logging.Level.FINE, "measurementsPropertiesTabFolder.close, event=" + evt); //$NON-NLS-1$
								//a measurement property gets removed
								PropertyTypeTabItem tabItem = ((PropertyTypeTabItem) evt.item);
								if (MeasurementTypeTabItem.this.deviceConfig != null) {
									MeasurementTypeTabItem.this.measurementType.getProperty().remove(tabItem.propertyType);
									MeasurementTypeTabItem.this.deviceConfig.setChangePropery(true);
									MeasurementTypeTabItem.this.propsEditor.enableSaveButton(true);
								}
								tabItem.dispose();
							}
						});
					}
					this.channelConfigMeasurementPropertiesTabFolder.setSelection(0);
					this.channelConfigMeasurementPropertiesTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
						@Override
						public void restore(CTabFolderEvent evt) {
							log.log(java.util.logging.Level.FINE, "channelConfigMeasurementPropertiesTabFolder.restore, event=" + evt); //$NON-NLS-1$
							((CTabItem) evt.item).getControl();
						}

						@Override
						public void close(CTabFolderEvent evt) {
							log.log(java.util.logging.Level.FINE, "channelConfigMeasurementPropertiesTabFolder.close, event=" + evt); //$NON-NLS-1$
							//Statistics or Properties(all) get removed 
							CTabItem tabItem = ((CTabItem) evt.item);
							if (MeasurementTypeTabItem.this.deviceConfig != null) {
								if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0350))) {
									MeasurementTypeTabItem.this.measurementType.setStatistics(null);
									MeasurementTypeTabItem.this.deviceConfig.setChangePropery(true);
								}
								else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0530))) {
									for (int j = 0; j < MeasurementTypeTabItem.this.measurementType.getProperty().size(); j++) {
										MeasurementTypeTabItem.this.measurementType.getProperty().remove(j);
										MeasurementTypeTabItem.this.deviceConfig.setChangePropery(true);
									}
									MeasurementTypeTabItem.this.deviceConfig.setChangePropery(true);
								}
								MeasurementTypeTabItem.this.propsEditor.enableSaveButton(true);
							}
							tabItem.dispose();
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

	/**
	 * 
	 */
	public void enableContextMenu(boolean enable) {
		log.log(java.util.logging.Level.FINE, "MeasurementTypeTabItem.this.enableContextMenu " + enable); //$NON-NLS-1$						
		if (enable && (this.popupMenu == null || this.contextMenu == null)) {
			this.popupMenu = new Menu(this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			//this.popupMenu = SWTResourceManager.getMenu("MeasurementContextMenu", this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			this.contextMenu = new MeasurementContextmenu(this.popupMenu, this, this.channelConfigMeasurementPropertiesTabFolder);
			this.contextMenu.create();
		}
		else if (!enable && this.popupMenu != null) {
			this.popupMenu.dispose();
			this.popupMenu = null;
			this.contextMenu = null;
		}
		this.measurementsComposite.setMenu(this.popupMenu);
		this.measurementTypeLabel.setMenu(this.popupMenu);
		this.measurementNameLabel.setMenu(this.popupMenu);
		this.measurementSymbolLabel.setMenu(this.popupMenu);
		this.measurementUnitLabel.setMenu(this.popupMenu);
		this.measurementEnableLabel.setMenu(this.popupMenu);

		this.channelConfigMeasurementPropertiesTabFolder.setMenu(this.popupMenu);
	}

	/**
	 * create measurement property tab item with its required sub tab folder
	 */
	private void createMeasurementPropertyTabItemWithSubTabFolder() {
		this.measurementPropertiesTabItem = new CTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
		this.measurementPropertiesTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0530));
		this.measurementPropertiesTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.channelConfigMeasurementPropertiesTabFolder.setSelection(this.measurementPropertiesTabItem);

		this.measurementPropertiesTabFolder = new CTabFolder(this.channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
		this.measurementPropertiesTabItem.setControl(this.measurementPropertiesTabFolder);
	}

	/**
	 * Create a new measurement property tab item
	 * Internally calling: PropertyTypeTabItem.setProperty(DeviceConfiguration, PropertyType, true, true, false, true);
	 * @param propertyTabItemName
	 * @return the created PropertyTypeTabItem
	 */
	public PropertyTypeTabItem createMeasurementPropertyTabItem(String propertyTabItemName) {
		//check, if a tab item and sub tab folder exist to create new PropertyType items
		if (this.measurementPropertiesTabFolder == null || this.measurementPropertiesTabFolder.isDisposed()) {
			this.createMeasurementPropertyTabItemWithSubTabFolder();
		}

		PropertyTypeTabItem tmpPropertyTypeTabItem = new PropertyTypeTabItem(this.measurementPropertiesTabFolder, SWT.CLOSE, propertyTabItemName, this);
		if (this.deviceConfig != null) {
			PropertyType tmpPropertyType = new ObjectFactory().createPropertyType();
			tmpPropertyType.setName(propertyTabItemName);
			switch (MeasurementPropertyTypes.fromValue(propertyTabItemName)) {
			case OFFSET:
				tmpPropertyType.setType(DataTypes.DOUBLE);
				tmpPropertyType.setValue(new Double("0.0")); //$NON-NLS-1$
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0535));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.DOUBLE.value() }, true);
				break;
			case FACTOR:
				tmpPropertyType.setType(DataTypes.DOUBLE);
				tmpPropertyType.setValue(1.0);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0536));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.DOUBLE.value() }, true);
				break;
			case REDUCTION:
				tmpPropertyType.setType(DataTypes.DOUBLE);
				tmpPropertyType.setValue(0.0);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0537));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.DOUBLE.value() }, true);
				break;
			case REGRESSION_INTERVAL_SEC:
				tmpPropertyType.setType(DataTypes.INTEGER);
				tmpPropertyType.setValue(15);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0538));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.INTEGER.value() }, true);
				break;
			case REGRESSION_TYPE_CURVE:
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(MeasurementPropertyTypes.REGRESSION_TYPE_CURVE.value());
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0539));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.STRING.value() }, true);
				break;
			case REGRESSION_TYPE_LINEAR:
				tmpPropertyType.setType(DataTypes.STRING);
				tmpPropertyType.setValue(MeasurementPropertyTypes.REGRESSION_TYPE_LINEAR.value());
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0540));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.STRING.value() }, true);
				break;
			case NUMBER_MOTOR:
				tmpPropertyType.setType(DataTypes.INTEGER);
				tmpPropertyType.setValue(1);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0541));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.INTEGER.value() }, true);
				break;
			case NUMBER_CELLS:
				tmpPropertyType.setType(DataTypes.INTEGER);
				tmpPropertyType.setValue(3);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0543));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.INTEGER.value() }, true);
				break;
			case PROP_N_100_W:
				tmpPropertyType.setType(DataTypes.INTEGER);
				tmpPropertyType.setValue(3400);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0544));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.INTEGER.value() }, true);
				break;
			case IS_INVERT_CURRENT:
				tmpPropertyType.setType(DataTypes.BOOLEAN);
				tmpPropertyType.setValue(false);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0545));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.BOOLEAN.value() }, true);
				break;
			case REVOLUTION_FACTOR:
				tmpPropertyType.setType(DataTypes.DOUBLE);
				tmpPropertyType.setValue(1.0);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0546));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, false, null, new String[] { DataTypes.DOUBLE.value() }, true);
				break;
			case NONE_SPECIFIED:
				tmpPropertyType.setType(DataTypes.DOUBLE);
				tmpPropertyType.setValue(1.0);
				tmpPropertyType.setDescription(Messages.getString(MessageIds.OSDE_MSGT0547));
				tmpPropertyTypeTabItem.setProperty(this.deviceConfig, tmpPropertyType, true, MeasurementPropertyTypes.valuesAsStingArray(), DataTypes.valuesAsStingArray(), true);
				break;
			}
			this.measurementType.getProperty().add(tmpPropertyType);
			this.deviceConfig.setChangePropery(true);
			this.propsEditor.enableSaveButton(true);
		}
		if (this.channelConfigMeasurementPropertiesTabFolder.isVisible()) {
			this.channelConfigMeasurementPropertiesTabFolder.setSelection(this.measurementPropertiesTabItem);
			this.measurementPropertiesTabFolder.setSelection(tmpPropertyTypeTabItem);
		}
		return tmpPropertyTypeTabItem;
	}

	/**
	 * Creates a new StatisticsTypeTabItem
	 * @return reference to created StatisticsTypeTabItem
	 */
	public StatisticsTypeTabItem createStatisticsTabItem() {
		StatisticsTypeTabItem tmpStatisticsTypeTabItem = this.statisticsTypeTabItem = new StatisticsTypeTabItem(this.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE | SWT.H_SCROLL, Messages
				.getString(MessageIds.OSDE_MSGT0350), this);
		if (this.channelConfigMeasurementPropertiesTabFolder.isVisible()) {
			this.channelConfigMeasurementPropertiesTabFolder.setSelection(tmpStatisticsTypeTabItem);
		}
		return tmpStatisticsTypeTabItem;
	}
}
