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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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

	final CTabFolder		measurementsTabFolder;
	final String				tabName;

	ScrolledComposite		scrolledComposite;
	Composite						statisticsComposite;
	CCombo							triggerLevelCombo;
	Label								triggerLevelLabel;
	CCombo							sumByTriggerRefOrdinalCombo;
	Button							isSumByTriggerRefOrdinalButton;
	Button							countByTriggerButton;
	Text								sumTriggerText;
	Button							isRatioRefOrdinalButton;
	Text								triggerCommentText;
	CCombo							minTimeSecCombo;
	Label								minTimeSecLabel;
	Button							isGreaterButton;
	Button							triggerLevelButton;
	Text								ratioText;
	CCombo							ratioRefOrdinalCombo;
	Text								countTriggerText;
	CCombo							triggerRefOrdinalCombo;
	Button							isTriggerRefOrdinalButton;
	Button							statisticsSigmaButton;
	Button							statisticsMaxButton;
	Button							statisticsAvgButton;
	Button							statisticsMinButton;

	boolean							statisticsMin, statisticsMax, statisticsAvg, statisticsSigma;
	String							triggerComment, sumTriggerComment, countTriggerComment, ratioComment;
	Boolean							isGreater, isCountByTrigger, isRatioRefOrdinal;
	Integer							triggerLevel, minTimeSec, triggerRefOrdinal, sumByTriggerRefOrdinal, ratioRefOrdinal;
	String[]						measurementReferenceItems;

	DeviceConfiguration	deviceConfig;
	int									channelConfigNumber;
	StatisticsType			statisticsType;
	TriggerType					triggerType;

	public StatisticsTypeTabItem(CTabFolder parent, int style, String name) {
		super(parent, style);
		this.measurementsTabFolder = parent;
		this.tabName = name;
		StatisticsTypeTabItem.log.log(Level.FINE, "StatisticsTypeTabItem " + name);
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
		this.triggerType = this.statisticsType.getTrigger();

		this.statisticsMinButton.setSelection(this.statisticsMin = this.statisticsType.isMin());
		this.statisticsMaxButton.setSelection(this.statisticsMax = this.statisticsType.isMax());
		this.statisticsAvgButton.setSelection(this.statisticsAvg = this.statisticsType.isAvg());
		this.statisticsSigmaButton.setSelection(this.statisticsSigma = this.statisticsType.isSigma());

		this.measurementReferenceItems = this.deviceConfig.getMeasurementNames(this.channelConfigNumber);

		if (this.triggerType != null) {
			this.triggerLevelButton.setSelection(true);
			this.triggerLevelLabel.setEnabled(true);
			this.triggerLevelCombo.setEnabled(true);
			this.triggerCommentText.setEnabled(true);
			this.isGreaterButton.setEnabled(true);
			this.minTimeSecCombo.setEnabled(true);
			this.triggerLevelCombo.setText(OSDE.STRING_EMPTY + (this.triggerLevel = this.triggerType.getLevel()));
			this.triggerCommentText.setText(this.triggerComment = this.triggerType.getComment());
			this.isGreaterButton.setSelection(this.isGreater = this.triggerType.isGreater());
			this.minTimeSecCombo.select((this.minTimeSec = this.triggerType.getMinTimeSec()) - 1);

			this.countByTriggerButton.setEnabled(true);
			if ((this.isCountByTrigger = this.statisticsType.isCountByTrigger()) != null) {
				this.countByTriggerButton.setSelection(this.isCountByTrigger);
				if (this.isCountByTrigger) {
					this.countTriggerText.setEnabled(true);
					this.countTriggerText.setText(this.countTriggerComment = this.statisticsType.getCountTriggerText());
				}
				else {
					this.countTriggerText.setEnabled(false);
				}
			}
			else {
				this.countTriggerText.setEnabled(false);
			}
		}
		else {
			this.triggerLevelButton.setSelection(false);
			this.triggerLevelLabel.setEnabled(false);
			this.triggerLevelCombo.setEnabled(false);
			this.triggerCommentText.setEnabled(false);
			this.isGreaterButton.setEnabled(false);
			this.minTimeSecCombo.setEnabled(false);

			this.countByTriggerButton.setEnabled(false);
			this.countTriggerText.setEnabled(false);
		}

		if ((this.triggerRefOrdinal = this.statisticsType.getTriggerRefOrdinal()) != null) {
			this.isTriggerRefOrdinalButton.setSelection(true);
			this.triggerRefOrdinalCombo.setEnabled(true);
			this.triggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
			this.triggerRefOrdinalCombo.select(this.triggerRefOrdinal);
		}
		else {
			this.triggerRefOrdinalCombo.setEnabled(false);
		}

		if ((this.sumByTriggerRefOrdinal = this.statisticsType.getSumByTriggerRefOrdinal()) != null) {
			this.isSumByTriggerRefOrdinalButton.setEnabled(true);
			this.isSumByTriggerRefOrdinalButton.setSelection(true);
			this.sumByTriggerRefOrdinalCombo.setEnabled(true);
			this.sumTriggerText.setEnabled(true);
			this.sumByTriggerRefOrdinalCombo.setItems(this.measurementReferenceItems);
			this.sumByTriggerRefOrdinalCombo.select(this.sumByTriggerRefOrdinal);
			this.sumTriggerText.setText(this.sumTriggerComment = this.statisticsType.getSumTriggerText());
		}
		else {
			this.isSumByTriggerRefOrdinalButton.setSelection(false);
			this.sumByTriggerRefOrdinalCombo.setEnabled(false);
			this.sumTriggerText.setEnabled(false);
		}

		if ((this.ratioRefOrdinal = this.statisticsType.getRatioRefOrdinal()) != null) {
			this.isRatioRefOrdinalButton.setEnabled(true);
			this.isRatioRefOrdinalButton.setSelection(this.isRatioRefOrdinal = true);
			this.ratioRefOrdinalCombo.setEnabled(true);
			this.ratioRefOrdinalCombo.setItems(this.measurementReferenceItems);
			this.ratioRefOrdinalCombo.select(this.ratioRefOrdinal);
			this.ratioText.setEnabled(true);
			this.ratioText.setText(this.ratioComment = this.statisticsType.getRatioText());
		}
		else {
			this.isRatioRefOrdinalButton.setSelection(this.isRatioRefOrdinal = false);
			this.ratioRefOrdinalCombo.setEnabled(false);
			this.ratioText.setEnabled(false);
		}
	}

	private void initGUI() {
		try {
			this.setText(this.tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			this.scrolledComposite = new ScrolledComposite(this.measurementsTabFolder, SWT.H_SCROLL);
			this.setControl(this.scrolledComposite);
			this.statisticsComposite = new Composite(this.scrolledComposite, SWT.NONE);
			this.statisticsComposite.setLayout(null);
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
						}
					}
				});
			}
			{
				this.triggerLevelButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.LEFT);
				this.triggerLevelButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerLevelButton.setText("trigger");
				this.triggerLevelButton.setBounds(125, 5, 60, 20);
				this.triggerLevelButton.setToolTipText("describes the value level where statistics calculation is active (4000 == 4A)");
				this.triggerLevelButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerLevelButton.widgetSelected, event=" + evt);
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							if (StatisticsTypeTabItem.this.triggerLevelButton.getSelection()) {
								if (StatisticsTypeTabItem.this.statisticsType.getTrigger() == null) {
									StatisticsTypeTabItem.this.statisticsType.setTrigger(StatisticsTypeTabItem.this.triggerType = new ObjectFactory().createTriggerType());
									StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
								}
								StatisticsTypeTabItem.this.triggerCommentText.setEnabled(true);
								StatisticsTypeTabItem.this.triggerLevelLabel.setEnabled(true);
								StatisticsTypeTabItem.this.triggerLevelCombo.setEnabled(true);
								StatisticsTypeTabItem.this.isGreaterButton.setEnabled(true);
								StatisticsTypeTabItem.this.minTimeSecLabel.setEnabled(true);
								StatisticsTypeTabItem.this.minTimeSecCombo.setEnabled(true);
								StatisticsTypeTabItem.this.countByTriggerButton.setEnabled(true);
								StatisticsTypeTabItem.this.countTriggerText.setEnabled(true);
							}
							else {
								StatisticsTypeTabItem.this.triggerCommentText.setEnabled(false);
								StatisticsTypeTabItem.this.triggerLevelLabel.setEnabled(false);
								StatisticsTypeTabItem.this.triggerLevelCombo.setEnabled(false);
								StatisticsTypeTabItem.this.isGreaterButton.setEnabled(false);
								StatisticsTypeTabItem.this.minTimeSecLabel.setEnabled(false);
								StatisticsTypeTabItem.this.minTimeSecCombo.setEnabled(false);
								StatisticsTypeTabItem.this.countByTriggerButton.setEnabled(false);
								StatisticsTypeTabItem.this.countTriggerText.setEnabled(false);
							}
						}
					}
				});
			}
			{
				this.triggerLevelLabel = new Label(this.statisticsComposite, SWT.RIGHT);
				this.triggerLevelLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerLevelLabel.setText("level*10E-3");
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
						StatisticsTypeTabItem.this.triggerLevel = StatisticsTypeTabItem.this.triggerLevelCombo.getSelectionIndex() + 1;
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
				this.isGreaterButton.setToolTipText("true means all values above trigger level will be counted");
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
						StatisticsTypeTabItem.this.minTimeSec = StatisticsTypeTabItem.this.minTimeSecCombo.getSelectionIndex() + 1;
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
				this.countByTriggerButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.countByTriggerButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.countByTriggerButton.setText("countByTrigger");
				this.countByTriggerButton.setBounds(125, 55, 170, 20);
				this.countByTriggerButton.setToolTipText("counts the number of events trigger level becomes active  at specified trigger type ");
				this.countByTriggerButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "countByTriggerButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.isCountByTrigger = StatisticsTypeTabItem.this.countByTriggerButton.getSelection();
						StatisticsTypeTabItem.this.countTriggerText.setEnabled(StatisticsTypeTabItem.this.isCountByTrigger);
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setCountByTrigger(StatisticsTypeTabItem.this.isCountByTrigger);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
							if (!StatisticsTypeTabItem.this.isCountByTrigger) {
								StatisticsTypeTabItem.this.statisticsType.setCountTriggerText(OSDE.STRING_EMPTY);
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
				this.isTriggerRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.isTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isTriggerRefOrdinalButton.setText("triggerRef");
				this.isTriggerRefOrdinalButton.setBounds(125, 80, 150, 20);
				this.isTriggerRefOrdinalButton.setToolTipText("references the measurement ordinal where trigger level is set in case of trigger is defined (0=VoltageReceiver;1=Voltage,2=Current, ...)");
				this.isTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isTriggerRefOrdinalButton.widgetSelected, event=" + evt);
						if (StatisticsTypeTabItem.this.isTriggerRefOrdinalButton.getSelection()) {
							StatisticsTypeTabItem.this.triggerRefOrdinalCombo.setEnabled(true);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
						else {
							StatisticsTypeTabItem.this.triggerRefOrdinalCombo.setEnabled(false);
							if (StatisticsTypeTabItem.this.statisticsType != null) {
								StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
								StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal = null);
							}
						}
					}
				});
			}
			{
				this.triggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.triggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.triggerRefOrdinalCombo.setBounds(280, 80, 75, 20);
				this.triggerRefOrdinalCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				this.triggerRefOrdinalCombo.setEditable(false);
				this.triggerRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "triggerRefOrdinalCombo.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.triggerRefOrdinal = StatisticsTypeTabItem.this.triggerRefOrdinalCombo.getSelectionIndex() + 1;
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setTriggerRefOrdinal(StatisticsTypeTabItem.this.triggerRefOrdinal);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.isSumByTriggerRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.isSumByTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isSumByTriggerRefOrdinalButton.setText("sumByTriggerRef");
				this.isSumByTriggerRefOrdinalButton.setBounds(125, 105, 150, 20);
				this.isSumByTriggerRefOrdinalButton.setToolTipText("calculates sum of values where trigger level becomes active at referenced triggered measurement");
				this.isSumByTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isSumByTriggerRefOrdinalButton.widgetSelected, event=" + evt);
						if (StatisticsTypeTabItem.this.isSumByTriggerRefOrdinalButton.getSelection()) {
							StatisticsTypeTabItem.this.sumByTriggerRefOrdinalCombo.setEnabled(true);
							StatisticsTypeTabItem.this.sumTriggerText.setEnabled(true);
						}
						else {
							StatisticsTypeTabItem.this.sumByTriggerRefOrdinalCombo.setEnabled(false);
							StatisticsTypeTabItem.this.sumTriggerText.setEnabled(false);
						}
						if (StatisticsTypeTabItem.this.deviceConfig != null) StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
					}
				});
			}
			{
				this.sumByTriggerRefOrdinalCombo = new CCombo(this.statisticsComposite, SWT.BORDER);
				this.sumByTriggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.sumByTriggerRefOrdinalCombo.setBounds(280, 105, 75, 20);
				this.sumByTriggerRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "sumByTriggerRefOrdinalCombo.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.sumByTriggerRefOrdinal = StatisticsTypeTabItem.this.sumByTriggerRefOrdinalCombo.getSelectionIndex();
						if (StatisticsTypeTabItem.this.statisticsType != null) {
							StatisticsTypeTabItem.this.statisticsType.setSumByTriggerRefOrdinal(StatisticsTypeTabItem.this.sumByTriggerRefOrdinal);
							StatisticsTypeTabItem.this.deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				this.sumTriggerText = new Text(this.statisticsComposite, SWT.BORDER);
				this.sumTriggerText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.sumTriggerText.setBounds(360, 105, 325, 20);
				this.sumTriggerText.setToolTipText("this is the text displayed in front of sum value in case of trigger is defined");
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
				this.isRatioRefOrdinalButton = new Button(this.statisticsComposite, SWT.CHECK | SWT.RIGHT);
				this.isRatioRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				this.isRatioRefOrdinalButton.setText("ratioRef");
				this.isRatioRefOrdinalButton.setBounds(125, 130, 150, 20);
				this.isRatioRefOrdinalButton.setToolTipText("measurement ordinal to calculate the ratio of referenced avg or max value to sumByTriggerRef");
				this.isRatioRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						StatisticsTypeTabItem.log.log(Level.FINEST, "isRatioRefOrdinalButton.widgetSelected, event=" + evt);
						StatisticsTypeTabItem.this.isRatioRefOrdinal = StatisticsTypeTabItem.this.isRatioRefOrdinalButton.getSelection();
						StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setEnabled(StatisticsTypeTabItem.this.isRatioRefOrdinal);
						StatisticsTypeTabItem.this.ratioRefOrdinalCombo.setItems(StatisticsTypeTabItem.this.measurementReferenceItems);
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
				this.ratioRefOrdinalCombo.setBounds(280, 130, 75, 20);
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

}
