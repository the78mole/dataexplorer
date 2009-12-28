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
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.DeviceConfiguration;
import osde.device.ObjectFactory;
import osde.device.StatisticsType;
import osde.device.TriggerType;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * class defining a composite with statistics configuration data
 * @author Winfried Brügmann
*/
public class StatisticsTypeTabItem extends CTabItem {
	final static Logger	log	= Logger.getLogger(StatisticsTypeTabItem.class.getName());

	final MeasurementTypeTabItem	measurementTypeTabItem;
	final CTabFolder							channelConfigMeasurementPropertiesTabFolder;
	final String									tabName;

	Menu													popupMenu;
	MeasurementContextmenu				contextMenu;

	ScrolledComposite							scrolledComposite;
	Composite											statisticsComposite;
	CCombo												triggerLevelCombo;
	Label													triggerLevelLabel;
	CCombo												sumByTriggerRefOrdinalCombo;
	Button												isSumByTriggerRefOrdinalButton;
	Button												countByTriggerButton;
	Text													sumTriggerText;
	Button												isRatioRefOrdinalButton;
	Text													triggerCommentText;
	CCombo												minTimeSecCombo;
	Label													minTimeSecLabel;
	Button												isGreaterButton;
	Button												triggerLevelButton;
	Text													ratioText;
	CCombo												ratioRefOrdinalCombo;
	Text													countTriggerText;
	CCombo												triggerRefOrdinalCombo;
	Button												isTriggerRefOrdinalButton;
	Button												statisticsSigmaButton;
	Button												statisticsMaxButton;
	Button												statisticsAvgButton;
	Button												statisticsMinButton;

	Boolean												statisticsMin, statisticsMax, statisticsAvg, statisticsSigma;
	String												triggerComment, sumTriggerComment, countTriggerComment, ratioComment;
	Boolean												isGreater, isCountByTrigger, isRatioRefOrdinal;
	Integer												triggerLevel, minTimeSec, ratioRefOrdinal;
	Integer												triggerRefOrdinal, sumByTriggerRefOrdinal;	// must be equal !!!
	boolean												isSomeTriggerDefined	= false;
	String[]											measurementReferenceItems;

	DeviceConfiguration	deviceConfig;
	int									channelConfigNumber;
	StatisticsType			statisticsType;
	TriggerType					triggerType;

	public StatisticsTypeTabItem(CTabFolder parent, int style, String name, MeasurementTypeTabItem useMeasurementTypeTabItem) {
		super(parent, style);
		this.channelConfigMeasurementPropertiesTabFolder = parent;
		this.measurementTypeTabItem = useMeasurementTypeTabItem;
		this.tabName = name;

		initGUI();
	}

	public synchronized StatisticsTypeTabItem clone() {
		return new StatisticsTypeTabItem(this);
	}
	
	/**
	 * copy constructor
	 * @param copyFrom
	 */
	private StatisticsTypeTabItem(StatisticsTypeTabItem copyFrom) {
		super(copyFrom.channelConfigMeasurementPropertiesTabFolder, SWT.CLOSE);
		this.channelConfigMeasurementPropertiesTabFolder = copyFrom.channelConfigMeasurementPropertiesTabFolder;
		this.measurementTypeTabItem = copyFrom.measurementTypeTabItem;
		this.tabName = copyFrom.tabName;

		this.statisticsMin = copyFrom.statisticsMin;
		this.statisticsMax = copyFrom.statisticsMax;
		this.statisticsAvg = copyFrom.statisticsAvg;
		this.statisticsSigma = copyFrom.statisticsSigma;
		
		//do not copy trigger attributes, only one trigger permitted per channel/configuration measurements
		//this.triggerLevel = copyFrom.triggerLevel;
		//this.triggerComment = copyFrom.triggerComment;
		//this.isGreater = copyFrom.isGreater;
		//this.minTimeSec = copyFrom.minTimeSec;
		this.isCountByTrigger = copyFrom.isCountByTrigger;
		this.countTriggerComment = copyFrom.countTriggerComment;

		this.triggerRefOrdinal = copyFrom.triggerRefOrdinal;
		this.sumByTriggerRefOrdinal = copyFrom.sumByTriggerRefOrdinal;
		this.sumTriggerComment = copyFrom.sumTriggerComment;
		this.isRatioRefOrdinal = copyFrom.isRatioRefOrdinal;
		this.ratioRefOrdinal = copyFrom.ratioRefOrdinal;
		this.ratioComment = copyFrom.ratioComment;
		
		this.deviceConfig = copyFrom.deviceConfig;
		this.channelConfigNumber = copyFrom.channelConfigNumber;	
		
		initGUI();
	}

	/**
	 * set the statistics type and update all internal variables
	 * @param statisticsType the statisticsType to set
	 */
	public void setStatisticsType(DeviceConfiguration useDeviceConfig, StatisticsType useStatisticsType, int useChannelConfigNumber) {
		this.deviceConfig = useDeviceConfig;
		this.statisticsType = useStatisticsType;
		this.channelConfigNumber = useChannelConfigNumber;
		this.triggerType = this.statisticsType == null ? null : this.statisticsType.getTrigger();

		this.statisticsMinButton.setSelection(this.statisticsMin = this.statisticsType.isMin());
		this.statisticsMaxButton.setSelection(this.statisticsMax = this.statisticsType.isMax());
		this.statisticsAvgButton.setSelection(this.statisticsAvg = this.statisticsType.isAvg());
		this.statisticsSigmaButton.setSelection(this.statisticsSigma = this.statisticsType.isSigma());

		this.measurementReferenceItems = this.deviceConfig.getMeasurementNames(this.channelConfigNumber);
		this.triggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
		this.sumByTriggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
		this.ratioRefOrdinalCombo.setItems(this.measurementReferenceItems);
		
		updateTriggerDependent(this.isSomeTriggerDefined = isSomeTriggerDefined());
		
		if (triggerRefOrdinal != sumByTriggerRefOrdinal) {
			MessageBox mb = new MessageBox(this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.OK);
			mb.setText("Warning message");
			mb.setMessage("triggerRefOrdinal != sumByTriggerRefOrdinal\nBoth ordinals can only reference the ordinal of the measurement where a trigger is defined!\nPlease correct.");
		}
	}

	/**
	 * Update trigger dependent attributes and fields
	 * A trigger reference can not be set if no trigger is defined for this measurement types!
	 * This method will remove trigger referencing or depending configurations if no trigger is defined.
	 */
	private void updateTriggerDependent(boolean isTriggerDefined) {
		this.triggerLevelButton.setEnabled(!isTriggerDefined || this.triggerType != null);

		this.triggerLevelButton.setSelection(isTriggerDefined && this.triggerType != null);
		this.triggerLevelLabel.setEnabled(isTriggerDefined && this.triggerType != null);
		this.triggerLevelCombo.setEnabled(isTriggerDefined && this.triggerType != null);
		this.triggerCommentText.setEnabled(isTriggerDefined && this.triggerType != null);
		this.isGreaterButton.setEnabled(isTriggerDefined && this.triggerType != null);
		this.minTimeSecLabel.setEnabled(isTriggerDefined && this.triggerType != null);
		this.minTimeSecCombo.setEnabled(isTriggerDefined && this.triggerType != null);
		
		if (isTriggerDefined && this.triggerType != null) {
			this.triggerLevelCombo.setText(OSDE.STRING_EMPTY + (this.triggerLevel = this.triggerType.getLevel()));
			this.triggerCommentText.setText(this.triggerComment = this.triggerType.getComment() == null ? OSDE.STRING_EMPTY : this.triggerType.getComment());
			this.isGreaterButton.setSelection(this.isGreater = this.triggerType.isGreater());
			this.minTimeSecCombo.select((this.minTimeSec = this.triggerType.getMinTimeSec()) - 1);
		}
		else if (this.statisticsType != null && this.statisticsType.getTrigger() != null){
			this.statisticsType.removeTrigger();
			this.deviceConfig.setChangePropery(true);
		}
		
		this.countByTriggerButton.setEnabled(isTriggerDefined);
		this.isTriggerRefOrdinalButton.setEnabled(isTriggerDefined && this.triggerType == null);
		this.isSumByTriggerRefOrdinalButton.setEnabled(isTriggerDefined && this.triggerType == null);
		this.sumTriggerText.setEnabled(isTriggerDefined && this.triggerType == null);
		this.isRatioRefOrdinalButton.setEnabled(isTriggerDefined && this.triggerType == null);
		this.ratioRefOrdinalCombo.setEnabled(isTriggerDefined && this.triggerType == null);
		this.ratioText.setEnabled(isTriggerDefined && this.triggerType == null);

		if (isTriggerDefined) {
			if ((this.isCountByTrigger = this.statisticsType.isCountByTrigger()) != null) {
				this.countByTriggerButton.setSelection(this.isCountByTrigger);
				if (this.isCountByTrigger) {
					this.countTriggerText.setEnabled(true);
					this.countTriggerText.setText(this.countTriggerComment = this.statisticsType.getCountTriggerText() == null ? OSDE.STRING_EMPTY : this.statisticsType.getCountTriggerText());
				}
				else {
					this.countTriggerText.setEnabled(false);
				}
			}
			else {
				this.countTriggerText.setEnabled(false);
			}
			if ((this.triggerRefOrdinal = this.statisticsType.getTriggerRefOrdinal()) != null) {
				this.isTriggerRefOrdinalButton.setSelection(true);
				//this.triggerRefOrdinalCombo.setEnabled(true);
				this.triggerRefOrdinalCombo.select(this.triggerRefOrdinal);
			}
			else {
				//this.triggerRefOrdinalCombo.setEnabled(false);
			}
			if ((this.sumByTriggerRefOrdinal = this.statisticsType.getSumByTriggerRefOrdinal()) != null) {
				this.isSumByTriggerRefOrdinalButton.setEnabled(true);
				this.isSumByTriggerRefOrdinalButton.setSelection(true);
				//this.sumByTriggerRefOrdinalCombo.setEnabled(true);
				this.sumTriggerText.setEnabled(true);
				this.sumByTriggerRefOrdinalCombo.select(this.sumByTriggerRefOrdinal);
				this.sumTriggerText.setText((this.sumTriggerComment = this.statisticsType.getSumTriggerText()) != null ? this.sumTriggerComment : OSDE.STRING_EMPTY);
			}
			else {
				this.isSumByTriggerRefOrdinalButton.setSelection(false);
				//this.sumByTriggerRefOrdinalCombo.setEnabled(false);
				this.sumTriggerText.setEnabled(false);
			}
			if ((this.ratioRefOrdinal = this.statisticsType.getRatioRefOrdinal()) != null) {
				this.isRatioRefOrdinalButton.setEnabled(true);
				this.isRatioRefOrdinalButton.setSelection(this.isRatioRefOrdinal = true);
				this.ratioRefOrdinalCombo.setEnabled(true);
				this.ratioRefOrdinalCombo.select(this.ratioRefOrdinal);
				this.ratioText.setEnabled(true);
				this.ratioText.setText((this.ratioComment = this.statisticsType.getRatioText()) != null ? this.ratioComment : OSDE.STRING_EMPTY);
			}
			else {
				this.isRatioRefOrdinalButton.setSelection(this.isRatioRefOrdinal = false);
				this.ratioRefOrdinalCombo.setEnabled(false);
				this.ratioText.setEnabled(false);
			}
		}
		else { // no measurement defines a trigger, as reult no measurement can reference to it
			this.isCountByTrigger = null;
			if (this.statisticsType.isCountByTrigger() != null) {
				this.statisticsType.setCountByTrigger(null);
				this.deviceConfig.setChangePropery(true);
			}
			this.countByTriggerButton.setSelection(false);
			this.countTriggerText.setEnabled(false);
			this.countTriggerText.setText(OSDE.STRING_EMPTY);
			this.countTriggerComment = null;
			if (this.statisticsType.getCountTriggerText() != null) {
				this.statisticsType.setCountTriggerText(null);
				this.deviceConfig.setChangePropery(true);
			}

			this.triggerRefOrdinal = null;
			if (this.statisticsType.getTriggerRefOrdinal() != null) {
				this.statisticsType.setTriggerRefOrdinal(null);
				this.deviceConfig.setChangePropery(true);
			}
			this.isTriggerRefOrdinalButton.setSelection(false);
			this.triggerRefOrdinalCombo.select(0);

			this.sumByTriggerRefOrdinal = null;
			if (this.statisticsType.getSumByTriggerRefOrdinal() != null) {
				this.statisticsType.setSumByTriggerRefOrdinal(null);
				this.deviceConfig.setChangePropery(true);
			}
			this.isSumByTriggerRefOrdinalButton.setSelection(false);
			this.sumByTriggerRefOrdinalCombo.select(0);
			this.sumTriggerComment = null;
			if (this.statisticsType.getSumTriggerText() != null) {
				this.statisticsType.setSumTriggerText(null);
				this.deviceConfig.setChangePropery(true);
			}
			this.sumTriggerText.setEnabled(false);
			this.sumTriggerText.setText(OSDE.STRING_EMPTY);

			this.ratioRefOrdinal = null;
			if (this.statisticsType.getRatioRefOrdinal() != null) {
				this.statisticsType.setRatioRefOrdinal(null);
				this.deviceConfig.setChangePropery(true);
			}
			this.isRatioRefOrdinalButton.setEnabled(false);
			this.isRatioRefOrdinalButton.setSelection(false);
			this.ratioRefOrdinalCombo.setEnabled(false);
			this.ratioRefOrdinalCombo.select(0);
			this.ratioComment = null;
			if (this.statisticsType.getRatioText() != null) {
				this.statisticsType.setRatioText(null);
				this.deviceConfig.setChangePropery(true);
			}
			this.ratioText.setEnabled(false);
			this.ratioText.setText(OSDE.STRING_EMPTY);
		}
	}

	/**
	 * find if some measurement defines a trigger to enable reference to it,
	 * loop through all measurement of the channel configuration
	 * @return true if trigger is defined
	 */
	private boolean isSomeTriggerDefined() {
		for (int i = 0; this.deviceConfig != null && i < this.deviceConfig.getMeasurementNames(this.channelConfigNumber).length; i++) {
			if (this.deviceConfig.getMeasurementStatistic(this.channelConfigNumber, i).getTrigger() != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * find reference ordinal of a defined trigger,
	 * loop through all measurement of the channel configuration
	 * @return the reference ordinal or -1 if no trigger is set
	 */
	private int getTriggerReferenceOrdinal() {
		for (int i = 0; this.deviceConfig != null && i < this.deviceConfig.getMeasurementNames(this.channelConfigNumber).length; i++) {
			if (this.deviceConfig.getMeasurementStatistic(this.channelConfigNumber, i).getTrigger() != null) {
				return i;
			}
		}
		return -1;
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			this.scrolledComposite = new ScrolledComposite(this.channelConfigMeasurementPropertiesTabFolder, SWT.H_SCROLL);
			this.setControl(this.scrolledComposite);
			this.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent disposeevent) {
					log.log(Level.FINEST, "statisticsTypeTabItem.widgetDisposed, event=" + disposeevent);
					StatisticsTypeTabItem.this.enableContextMenu(false);
				}
			});
			this.statisticsComposite = new Composite(this.scrolledComposite, SWT.NONE);
			this.statisticsComposite.setLayout(null);
			this.statisticsComposite.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "statisticsComposite.paintControl, event=" + evt);
					if (StatisticsTypeTabItem.this.statisticsComposite.isVisible()) {
						StatisticsTypeTabItem.this.statisticsMinButton.setSelection(StatisticsTypeTabItem.this.statisticsMin == null ? StatisticsTypeTabItem.this.statisticsMin = false
								: StatisticsTypeTabItem.this.statisticsMin);
						StatisticsTypeTabItem.this.statisticsAvgButton.setSelection(StatisticsTypeTabItem.this.statisticsAvg == null ? StatisticsTypeTabItem.this.statisticsAvg = false
								: StatisticsTypeTabItem.this.statisticsAvg);
						StatisticsTypeTabItem.this.statisticsMaxButton.setSelection(StatisticsTypeTabItem.this.statisticsMax == null ? StatisticsTypeTabItem.this.statisticsMax = false
								: StatisticsTypeTabItem.this.statisticsMax);
						StatisticsTypeTabItem.this.statisticsSigmaButton.setSelection(StatisticsTypeTabItem.this.statisticsSigma == null ? StatisticsTypeTabItem.this.statisticsSigma = false
								: StatisticsTypeTabItem.this.statisticsSigma);
						
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.updateTriggerDependent(StatisticsTypeTabItem.this.isSomeTriggerDefined = StatisticsTypeTabItem.this.isSomeTriggerDefined());
						}
						if (StatisticsTypeTabItem.this.triggerLevel != null) {
							StatisticsTypeTabItem.this.triggerLevelButton.setSelection(StatisticsTypeTabItem.this.triggerLevel != null);
							StatisticsTypeTabItem.this.triggerLevelCombo.select(StatisticsTypeTabItem.this.triggerLevel == null ? 0 : StatisticsTypeTabItem.this.triggerLevel);
							StatisticsTypeTabItem.this.triggerCommentText.setText(StatisticsTypeTabItem.this.triggerComment == null ? OSDE.STRING_EMPTY : StatisticsTypeTabItem.this.triggerComment);
							StatisticsTypeTabItem.this.isGreaterButton
									.setSelection(StatisticsTypeTabItem.this.isGreater == null ? StatisticsTypeTabItem.this.isGreater = true : StatisticsTypeTabItem.this.isGreater);
							StatisticsTypeTabItem.this.minTimeSecCombo
									.select((StatisticsTypeTabItem.this.minTimeSec == null ? StatisticsTypeTabItem.this.minTimeSec = 1 : StatisticsTypeTabItem.this.minTimeSec) - 1);
						}
						StatisticsTypeTabItem.this.countByTriggerButton.setSelection(StatisticsTypeTabItem.this.isCountByTrigger == null ? false : StatisticsTypeTabItem.this.isCountByTrigger);
						StatisticsTypeTabItem.this.countTriggerText.setText(StatisticsTypeTabItem.this.countTriggerComment == null ? OSDE.STRING_EMPTY : StatisticsTypeTabItem.this.countTriggerComment);
						StatisticsTypeTabItem.this.isTriggerRefOrdinalButton.setSelection(StatisticsTypeTabItem.this.triggerRefOrdinal != null);
						StatisticsTypeTabItem.this.triggerRefOrdinalCombo.select(StatisticsTypeTabItem.this.triggerRefOrdinal == null ? StatisticsTypeTabItem.this.getTriggerReferenceOrdinal()
								: StatisticsTypeTabItem.this.triggerRefOrdinal);
						StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.setSelection(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal != null);
						StatisticsTypeTabItem.this.sumByTriggerRefOrdinalCombo.select(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal == null ? StatisticsTypeTabItem.this.getTriggerReferenceOrdinal()
								: StatisticsTypeTabItem.this.sumByTriggerRefOrdinal);
						StatisticsTypeTabItem.this.sumTriggerText.setText(StatisticsTypeTabItem.this.sumTriggerComment == null ? OSDE.STRING_EMPTY : StatisticsTypeTabItem.this.sumTriggerComment);
						StatisticsTypeTabItem.this.isRatioRefOrdinalButton.setSelection(StatisticsTypeTabItem.this.ratioRefOrdinal != null);
						StatisticsTypeTabItem.this.ratioRefOrdinalCombo.select(StatisticsTypeTabItem.this.ratioRefOrdinal == null ? 0 : StatisticsTypeTabItem.this.ratioRefOrdinal);
						StatisticsTypeTabItem.this.ratioText.setText(StatisticsTypeTabItem.this.ratioComment == null ? OSDE.STRING_EMPTY : StatisticsTypeTabItem.this.ratioComment);
						
						StatisticsTypeTabItem.this.enableContextMenu(true);
					}
				}
			});
			{
				this.statisticsMinButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.statisticsMinButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.statisticsMinButton.setText("minimum");
				this.statisticsMinButton.setBounds(10, 5, 90, 20);
				this.statisticsMinButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "staisticsMinButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.statisticsMin = StatisticsTypeTabItem.this.statisticsMinButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setMin(StatisticsTypeTabItem.this.statisticsMin);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.statisticsAvgButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.statisticsAvgButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.statisticsAvgButton.setText("average");
				this.statisticsAvgButton.setBounds(10, 30, 90, 20);
				this.statisticsAvgButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "statisticsAvgButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.statisticsAvg = StatisticsTypeTabItem.this.statisticsAvgButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setAvg(StatisticsTypeTabItem.this.statisticsAvg);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.statisticsMaxButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.statisticsMaxButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.statisticsMaxButton.setText("maximum");
				this.statisticsMaxButton.setBounds(10, 55, 90, 20);
				this.statisticsMaxButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "statisticsMaxButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.statisticsMax = StatisticsTypeTabItem.this.statisticsMaxButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setMax(StatisticsTypeTabItem.this.statisticsMax);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.statisticsSigmaButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.statisticsSigmaButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.statisticsSigmaButton.setText("sigma");
				this.statisticsSigmaButton.setBounds(10, 80, 90, 20);
				this.statisticsSigmaButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "statisticsSigmaButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.statisticsSigma = StatisticsTypeTabItem.this.statisticsSigmaButton.getSelection();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSigma(StatisticsTypeTabItem.this.statisticsSigma);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.triggerLevelButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.triggerLevelButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerLevelButton.setText("trigger");
				this.triggerLevelButton.setBounds(125, 5, 60, 20);
				this.triggerLevelButton.setToolTipText("describes the value level where statistics calculation is active (4000[unit*10E-3] == 4[unit])");
				this.triggerLevelButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerLevelButton.widgetSelected, event=" + evt);
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							if (StatisticsTypeTabItem.this.triggerLevelButton.getSelection()) {
								if (StatisticsTypeTabItem.this.statisticsType.getTrigger() == null) {
									StatisticsTypeTabItem.this.statisticsType.setTrigger(StatisticsTypeTabItem.this.triggerType = new ObjectFactory().createTriggerType());
									StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
									
									StatisticsTypeTabItem.this.triggerType.setLevel(StatisticsTypeTabItem.this.triggerLevel == null ? StatisticsTypeTabItem.this.triggerLevel=0 : StatisticsTypeTabItem.this.triggerLevel);
									StatisticsTypeTabItem.this.triggerType.setComment(StatisticsTypeTabItem.this.triggerComment);
									StatisticsTypeTabItem.this.triggerType.setIsGreater(StatisticsTypeTabItem.this.isGreater == null ? (StatisticsTypeTabItem.this.isGreater=true) : StatisticsTypeTabItem.this.isGreater);
									StatisticsTypeTabItem.this.triggerType.setMinTimeSec(StatisticsTypeTabItem.this.minTimeSec == null ? StatisticsTypeTabItem.this.minTimeSec=1 : StatisticsTypeTabItem.this.minTimeSec);
									StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
									StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(StatisticsTypeTabItem.this.countTriggerComment);
									
									StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setSumTriggerText(null);
									StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(null);
									StatisticsTypeTabItem.this.statisticsType.setRatioText(null);
									
									StatisticsTypeTabItem.this.statisticsComposite.redraw();
								}
								
								StatisticsTypeTabItem.this.isTriggerRefOrdinalButton.setEnabled(false);
								StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.setEnabled(false);
								StatisticsTypeTabItem.this.sumTriggerText.setEnabled(false);
								StatisticsTypeTabItem.this.isRatioRefOrdinalButton.setEnabled(false);
								StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(false);
								StatisticsTypeTabItem.this.ratioText.setEnabled(false);
							}
							else {
								StatisticsTypeTabItem.this.statisticsType.removeTrigger();
								StatisticsTypeTabItem.this.triggerType = null;
								StatisticsTypeTabItem.this.triggerLevel = null;
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);

								StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
								StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setSumTriggerText(StatisticsTypeTabItem.this.sumTriggerComment);
								StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(StatisticsTypeTabItem.this.ratioRefOrdinal);
								StatisticsTypeTabItem.this.statisticsType.setRatioText(StatisticsTypeTabItem.this.ratioComment);

								StatisticsTypeTabItem.this.triggerCommentText.setEnabled(false);
								StatisticsTypeTabItem.this.triggerLevelLabel.setEnabled(false);
								StatisticsTypeTabItem.this.triggerLevelCombo.setEnabled(false);
								StatisticsTypeTabItem.this.isGreaterButton.setEnabled(false);
								StatisticsTypeTabItem.this.minTimeSecLabel.setEnabled(false);
								StatisticsTypeTabItem.this.minTimeSecCombo.setEnabled(false);
								StatisticsTypeTabItem.this.countByTriggerButton.setEnabled(false);
								StatisticsTypeTabItem.this.countTriggerText.setEnabled(false);

								StatisticsTypeTabItem.this.isTriggerRefOrdinalButton.setEnabled(true);
								StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.setEnabled(true);
								StatisticsTypeTabItem.this.sumTriggerText.setEnabled(true);
								StatisticsTypeTabItem.this.isRatioRefOrdinalButton.setEnabled(true);
								StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(true);
								StatisticsTypeTabItem.this.ratioText.setEnabled(true);

								StatisticsTypeTabItem.this.statisticsComposite.redraw();
							}
						}
					}
				});
			}
			{
				this.triggerLevelLabel = new Label(this.statisticsComposite, SWT.RIGHT);
				this.triggerLevelLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerLevelLabel.setText("unit*10E-3");
				this.triggerLevelLabel.setBounds(212, 5, 64, 20);
			}
			{
				this.triggerLevelCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.triggerLevelCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerLevelCombo.setBounds(280, 5, 75, 20);
				this.triggerLevelCombo.setToolTipText("minimum time length in seconds of trigger range to filter short time periods");
				this.triggerLevelCombo.setItems(new String[] { "250", "500", "1000", "1500", "2000", "2500", "3000", "3500", "4000", "5000", "7500" });
				this.triggerLevelCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerLevelCombo.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.triggerLevel = Integer.valueOf(StatisticsTypeTabItem.this.triggerLevelCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setLevel(StatisticsTypeTabItem.this.triggerLevel);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
				this.triggerLevelCombo.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerLevelCombo.keyReleased, event=" + evt);
						StatisticsTypeTabItem.this.triggerLevel = Integer.valueOf(StatisticsTypeTabItem.this.triggerLevelCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setLevel(StatisticsTypeTabItem.this.triggerLevel);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
				this.triggerLevelCombo.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerLevelCombo.verifyText, event=" + evt);
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.triggerCommentText = new Text(this.statisticsComposite, SWT.BORDER);
				this.triggerCommentText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerCommentText.setBounds(360, 5, 325, 20);
				this.triggerCommentText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerCommentText.keyReleased, event=" + evt);
						StatisticsTypeTabItem.this.triggerComment = StatisticsTypeTabItem.this.triggerCommentText.getText();
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setComment(StatisticsTypeTabItem.this.triggerComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.isGreaterButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isGreaterButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isGreaterButton.setText("isGreater");
				this.isGreaterButton.setBounds(125, 30, 75, 20);
				this.isGreaterButton.setToolTipText("true means all values above trigger level will be counted, this is the default");
				this.isGreaterButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isGreaterButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.isGreater = StatisticsTypeTabItem.this.isGreaterButton.getSelection();
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setIsGreater(StatisticsTypeTabItem.this.isGreater);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.minTimeSecLabel = new Label(this.statisticsComposite, SWT.RIGHT);
				this.minTimeSecLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.minTimeSecLabel.setText("minTimeSec");
				this.minTimeSecLabel.setBounds(200, 32, 75, 20);
			}
			{
				this.minTimeSecCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.minTimeSecCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.minTimeSecCombo.setBounds(280, 30, 75, 20);
				this.minTimeSecCombo.setToolTipText("minimum time length in seconds of trigger range to filter short time periods");
				this.minTimeSecCombo.setItems(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" });
				this.minTimeSecCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "minTimeSecCombo.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.minTimeSec = Integer.valueOf(StatisticsTypeTabItem.this.minTimeSecCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setMinTimeSec(StatisticsTypeTabItem.this.minTimeSec);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
				this.minTimeSecCombo.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "minTimeSecCombo.keyReleased, event=" + evt);
						StatisticsTypeTabItem.this.minTimeSec = Integer.valueOf(StatisticsTypeTabItem.this.minTimeSecCombo.getText());
						if (StatisticsTypeTabItem.this.triggerType != null) {
							StatisticsTypeTabItem.this.triggerType.setMinTimeSec(StatisticsTypeTabItem.this.minTimeSec);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
				this.minTimeSecCombo.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "minTimeSecCombo.verifyText, event=" + evt);
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.countByTriggerButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.countByTriggerButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.countByTriggerButton.setText("countByTrigger");
				this.countByTriggerButton.setBounds(125, 55, 170, 20);
				this.countByTriggerButton.setToolTipText("counts the number of events trigger level becomes active at specified trigger type");
				this.countByTriggerButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "countByTriggerButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.isCountByTrigger = StatisticsTypeTabItem.this.countByTriggerButton.getSelection() == false ? null : true;
						StatisticsTypeTabItem.this.countTriggerText.setEnabled(StatisticsTypeTabItem.this.isCountByTrigger != null);
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							if (StatisticsTypeTabItem.this.isCountByTrigger == null) {
								StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(null);
							}
							else {
								if (statisticsType.getTrigger() == null) {
									StatisticsTypeTabItem.this.isTriggerRefOrdinalButton.setSelection(true);
									handleTriggerRefOrdinalSelectionEvent();
								}
							}
						}
					}
				});
			}
			{
				this.countTriggerText = new Text(this.statisticsComposite, SWT.BORDER);
				this.countTriggerText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.countTriggerText.setBounds(360, 55, 325, 20);
				this.countTriggerText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "countTriggerText.keyReleased, event=" + evt);
						StatisticsTypeTabItem.this.countTriggerComment = StatisticsTypeTabItem.this.countTriggerText.getText();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(StatisticsTypeTabItem.this.countTriggerComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.isTriggerRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isTriggerRefOrdinalButton.setText("triggerRef");
				this.isTriggerRefOrdinalButton.setBounds(125, 80, 118, 20);
				this.isTriggerRefOrdinalButton.setToolTipText("References the measurement ordinal where trigger level is set.\nAll statistics values are calculated only with values where the trigger is active.\nA defined trigger is prerequisite.");
				this.isTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isTriggerRefOrdinalButton.widgetSelected, event=" + evt);
						handleTriggerRefOrdinalSelectionEvent();
					}
				});
			}
			{
				this.triggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.triggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerRefOrdinalCombo.setBounds(245, 80, 110, 20);
				this.triggerRefOrdinalCombo.setEnabled(false);
				this.triggerRefOrdinalCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				this.triggerRefOrdinalCombo.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			}
			{
				this.isSumByTriggerRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isSumByTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isSumByTriggerRefOrdinalButton.setText("sumByTriggerRef");
				this.isSumByTriggerRefOrdinalButton.setBounds(125, 105, 118, 20);
				this.isSumByTriggerRefOrdinalButton.setToolTipText("Calculates sum of values where the referenced trigger becomes active.\nA defined trigger is prerequisite.");
				this.isSumByTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isSumByTriggerRefOrdinalButton.widgetSelected, event=" + evt);
						if (StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.getSelection()) {
							StatisticsTypeTabItem.this.sumByTriggerRefOrdinal = StatisticsTypeTabItem.this.getTriggerReferenceOrdinal();
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							}
							StatisticsTypeTabItem.this.sumTriggerText.setEnabled(true);
						}
						else {
							StatisticsTypeTabItem.this.sumByTriggerRefOrdinal = null;
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							}
							StatisticsTypeTabItem.this.sumTriggerText.setEnabled(false);
						}
					}
				});
			}
			{
				this.sumByTriggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.sumByTriggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.sumByTriggerRefOrdinalCombo.setBounds(245, 105, 110, 20);
				this.sumByTriggerRefOrdinalCombo.setEnabled(false);
				this.sumByTriggerRefOrdinalCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				this.sumByTriggerRefOrdinalCombo.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			}
			{
				this.sumTriggerText = new Text(this.statisticsComposite, SWT.BORDER);
				this.sumTriggerText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.sumTriggerText.setBounds(360, 105, 325, 20);
				this.sumTriggerText.setToolTipText("This text is displayed in front of sum value as description.");
				this.sumTriggerText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "sumTriggerText.keyReleased, event=" + evt);
						StatisticsTypeTabItem.this.sumTriggerComment = StatisticsTypeTabItem.this.sumTriggerText.getText();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSumTriggerText(StatisticsTypeTabItem.this.sumTriggerComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.isRatioRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.isRatioRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isRatioRefOrdinalButton.setText("ratioRef");
				this.isRatioRefOrdinalButton.setBounds(125, 130, 118, 20);
				this.isRatioRefOrdinalButton.setToolTipText("Measurement ordinal to calculate the ratio of referenced avg or max value to sumByTriggerRef.");
				this.isRatioRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isRatioRefOrdinalButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.isRatioRefOrdinal = StatisticsTypeTabItem.this.isRatioRefOrdinalButton.getSelection();
						StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(StatisticsTypeTabItem.this.isRatioRefOrdinal);
						StatisticsTypeTabItem.this.ratioText.setEnabled(StatisticsTypeTabItem.this.isRatioRefOrdinal);
						if (StatisticsTypeTabItem.this.statisticsType != null && !StatisticsTypeTabItem.this.isRatioRefOrdinal) {
							StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(null);
							StatisticsTypeTabItem.this.ratioText.setText(OSDE.STRING_EMPTY);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.ratioRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.ratioRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.ratioRefOrdinalCombo.setBounds(245, 130, 110, 20);
				this.ratioRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "ratioRefOrdinalCombo.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.ratioRefOrdinal = StatisticsTypeTabItem.this.ratioRefOrdinalCombo.getSelectionIndex();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setRatioRefOrdinal(StatisticsTypeTabItem.this.ratioRefOrdinal);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.ratioText = new Text(this.statisticsComposite, SWT.BORDER);
				this.ratioText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.ratioText.setBounds(360, 130, 325, 20);
				this.ratioText.setToolTipText("text displayed in front of the calculated ratio case of resolvable dependencyes");
				this.ratioText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "ratioText.keyReleased, event=" + evt);
						StatisticsTypeTabItem.this.ratioComment = StatisticsTypeTabItem.this.ratioText.getText();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setRatioText(StatisticsTypeTabItem.this.ratioComment);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			this.scrolledComposite.setContent(this.statisticsComposite);
			this.statisticsComposite.setSize(700, 160);
			this.statisticsComposite.layout();

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * enable the context menu to create missing tab items
	 * @param enable
	 */
	public void enableContextMenu(boolean enable) {
		if (enable && (this.popupMenu == null || this.contextMenu == null)) {
			this.popupMenu = new Menu(this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			//this.popupMenu = SWTResourceManager.getMenu("MeasurementContextmenu", this.channelConfigMeasurementPropertiesTabFolder.getShell(), SWT.POP_UP);
			this.contextMenu = new MeasurementContextmenu(this.popupMenu, this.measurementTypeTabItem, this.channelConfigMeasurementPropertiesTabFolder);
			this.contextMenu.create();
		}
		else if (this.popupMenu != null) {
			this.popupMenu.dispose();
			this.popupMenu = null;
			this.contextMenu = null;
		}
		this.statisticsComposite.setMenu(this.popupMenu);
		this.statisticsMinButton.setMenu(this.popupMenu);
		this.statisticsAvgButton.setMenu(this.popupMenu);
		this.statisticsMaxButton.setMenu(this.popupMenu);
		this.statisticsSigmaButton.setMenu(this.popupMenu);
		this.triggerLevelButton.setMenu(this.popupMenu);
		this.triggerLevelLabel.setMenu(this.popupMenu);
		this.isGreaterButton.setMenu(this.popupMenu);
		this.minTimeSecLabel.setMenu(this.popupMenu);
		this.countByTriggerButton.setMenu(this.popupMenu);
		this.isTriggerRefOrdinalButton.setMenu(this.popupMenu);
		this.isSumByTriggerRefOrdinalButton.setMenu(this.popupMenu);
		this.isRatioRefOrdinalButton.setMenu(this.popupMenu);
	}

	/**
	 * Handle the event while isTriggerRefOrdinalButton is selected
	 * If trigger of this measurement is not defined a reference to the measurement defining the trigger is required
	 */
	private void handleTriggerRefOrdinalSelectionEvent() {
		if (StatisticsTypeTabItem.this.isTriggerRefOrdinalButton.getSelection()) {
			//StatisticsTypeTabItem.this.triggerRefOrdinalCombo.setEnabled(true);
			StatisticsTypeTabItem.this.triggerRefOrdinal = StatisticsTypeTabItem.this.getTriggerReferenceOrdinal();
			if (StatisticsTypeTabItem.this.statisticsType != null) {
				StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
				StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
			}
		}
		else {
			//StatisticsTypeTabItem.this.triggerRefOrdinalCombo.setEnabled(false);
			if (StatisticsTypeTabItem.this.statisticsType != null) {
				StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
				StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal = null);
			}
		}
	}

}
