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
		measurementsTabFolder = parent;
		tabName = name;
		System.out.println("StatisticsTypeTabItem " + name);
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
		this.triggerType = statisticsType.getTrigger();

		statisticsMinButton.setSelection(statisticsMin = statisticsType.isMin());
		statisticsMaxButton.setSelection(statisticsMax = statisticsType.isMax());
		statisticsAvgButton.setSelection(statisticsAvg = statisticsType.isAvg());
		statisticsSigmaButton.setSelection(statisticsSigma = statisticsType.isSigma());

		measurementReferenceItems = deviceConfig.getMeasurementNames(channelConfigNumber);

		if (triggerType != null) {
			triggerLevelButton.setSelection(true);
			triggerLevelLabel.setEnabled(true);
			triggerLevelCombo.setEnabled(true);
			triggerCommentText.setEnabled(true);
			isGreaterButton.setEnabled(true);
			minTimeSecCombo.setEnabled(true);
			triggerLevelCombo.setText(OSDE.STRING_EMPTY+(triggerLevel = triggerType.getLevel()));
			triggerCommentText.setText(triggerComment = triggerType.getComment());
			isGreaterButton.setSelection(isGreater = triggerType.isGreater());
			minTimeSecCombo.select((minTimeSec = triggerType.getMinTimeSec()) - 1);
			
			countByTriggerButton.setEnabled(true);
			if ((isCountByTrigger = statisticsType.isCountByTrigger()) != null) {
				countByTriggerButton.setSelection(isCountByTrigger);
				if (isCountByTrigger) {
					countTriggerText.setEnabled(true);
					countTriggerText.setText(countTriggerComment = statisticsType.getCountTriggerText());
				}
				else {
					countTriggerText.setEnabled(false);
				}
			}
			else {
				countTriggerText.setEnabled(false);
			}
		}
		else {
			triggerLevelButton.setSelection(false);
			triggerLevelLabel.setEnabled(false);
			triggerLevelCombo.setEnabled(false);
			triggerCommentText.setEnabled(false);
			isGreaterButton.setEnabled(false);
			minTimeSecCombo.setEnabled(false);

			countByTriggerButton.setEnabled(false);
			countTriggerText.setEnabled(false);
		}

		if ((triggerRefOrdinal = statisticsType.getTriggerRefOrdinal()) != null) {
			isTriggerRefOrdinalButton.setSelection(true);
			triggerRefOrdinalCombo.setEnabled(true);
			triggerRefOrdinalCombo.setItems(measurementReferenceItems);
			triggerRefOrdinalCombo.select(triggerRefOrdinal);
		}
		else {
			triggerRefOrdinalCombo.setEnabled(false);
		}

		if ((sumByTriggerRefOrdinal = statisticsType.getSumByTriggerRefOrdinal()) != null) {
			isSumByTriggerRefOrdinalButton.setEnabled(true);
			isSumByTriggerRefOrdinalButton.setSelection(true);
			sumByTriggerRefOrdinalCombo.setEnabled(true);
			sumTriggerText.setEnabled(true);
			sumByTriggerRefOrdinalCombo.setItems(measurementReferenceItems);
			sumByTriggerRefOrdinalCombo.select(sumByTriggerRefOrdinal);
			sumTriggerText.setText(sumTriggerComment = statisticsType.getSumTriggerText());
		}
		else {
			isSumByTriggerRefOrdinalButton.setSelection(false);
			sumByTriggerRefOrdinalCombo.setEnabled(false);
			sumTriggerText.setEnabled(false);
		}

		if ((ratioRefOrdinal = statisticsType.getRatioRefOrdinal()) != null) {
			isRatioRefOrdinalButton.setEnabled(true);
			isRatioRefOrdinalButton.setSelection(isRatioRefOrdinal = true);
			ratioRefOrdinalCombo.setEnabled(true);
			ratioRefOrdinalCombo.setItems(measurementReferenceItems);
			ratioRefOrdinalCombo.select(ratioRefOrdinal);
			ratioText.setEnabled(true);
			ratioText.setText(ratioComment = statisticsType.getRatioText());
		}
		else {
			isRatioRefOrdinalButton.setSelection(isRatioRefOrdinal = false);
			ratioRefOrdinalCombo.setEnabled(false);
			ratioText.setEnabled(false);
		}
	}

	private void initGUI() {
		try {
			this.setText(tabName);
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			scrolledComposite = new ScrolledComposite(measurementsTabFolder, SWT.H_SCROLL);
			this.setControl(scrolledComposite);
			statisticsComposite = new Composite(scrolledComposite, SWT.NONE);
			statisticsComposite.setLayout(null);
			{
				statisticsMinButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				statisticsMinButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				statisticsMinButton.setText("minimum");
				statisticsMinButton.setBounds(10, 5, 90, 20);
				statisticsMinButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "staisticsMinButton.widgetSelected, event=" + evt);
						statisticsMin = statisticsMinButton.getSelection();
						if (statisticsType != null) {
							statisticsType.setMin(statisticsMin);
						}
					}
				});
			}
			{
				statisticsAvgButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				statisticsAvgButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				statisticsAvgButton.setText("average");
				statisticsAvgButton.setBounds(10, 30, 90, 20);
				statisticsAvgButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "statisticsAvgButton.widgetSelected, event=" + evt);
						statisticsAvg = statisticsAvgButton.getSelection();
						if (statisticsType != null) {
							statisticsType.setAvg(statisticsAvg);
						}
					}
				});
			}
			{
				statisticsMaxButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				statisticsMaxButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				statisticsMaxButton.setText("maximum");
				statisticsMaxButton.setBounds(10, 55, 90, 20);
				statisticsMaxButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "statisticsMaxButton.widgetSelected, event=" + evt);
						statisticsMax = statisticsMaxButton.getSelection();
						if (statisticsType != null) {
							statisticsType.setMax(statisticsMax);
						}
					}
				});
			}
			{
				statisticsSigmaButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				statisticsSigmaButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				statisticsSigmaButton.setText("sigma");
				statisticsSigmaButton.setBounds(10, 80, 90, 20);
				statisticsSigmaButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "statisticsSigmaButton.widgetSelected, event=" + evt);
						statisticsSigma = statisticsSigmaButton.getSelection();
						if (statisticsType != null) {
							statisticsType.setSigma(statisticsSigma);
						}
					}
				});
			}
			{
				triggerLevelButton = new Button(statisticsComposite, SWT.CHECK | SWT.LEFT);
				triggerLevelButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				triggerLevelButton.setText("trigger");
				triggerLevelButton.setBounds(125, 5, 60, 20);
				triggerLevelButton.setToolTipText("describes the value level where statistics calculation is active (4000 == 4A)");
				triggerLevelButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "triggerLevelButton.widgetSelected, event=" + evt);
						if (statisticsType != null) {
							if (triggerLevelButton.getSelection()) {
								if (statisticsType.getTrigger() == null) {
									statisticsType.setTrigger(triggerType = new ObjectFactory().createTriggerType());
									deviceConfig.setChangePropery(true);
								}
								triggerCommentText.setEnabled(true);
								triggerLevelLabel.setEnabled(true);
								triggerLevelCombo.setEnabled(true);
								isGreaterButton.setEnabled(true);
								minTimeSecLabel.setEnabled(true);
								minTimeSecCombo.setEnabled(true);
								countByTriggerButton.setEnabled(true);
								countTriggerText.setEnabled(true);
							}
							else {
								triggerCommentText.setEnabled(false);
								triggerLevelLabel.setEnabled(false);
								triggerLevelCombo.setEnabled(false);
								isGreaterButton.setEnabled(false);
								minTimeSecLabel.setEnabled(false);
								minTimeSecCombo.setEnabled(false);
								countByTriggerButton.setEnabled(false);
								countTriggerText.setEnabled(false);
							}
						}
					}
				});
			}
			{
				triggerLevelLabel = new Label(statisticsComposite, SWT.RIGHT);
				triggerLevelLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				triggerLevelLabel.setText("level*10E-3");
				triggerLevelLabel.setBounds(212, 5, 64, 20);
			}
			{
				triggerLevelCombo = new CCombo(statisticsComposite, SWT.BORDER);
				triggerLevelCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				triggerLevelCombo.setBounds(280, 5, 75, 20);
				triggerLevelCombo.setToolTipText("minimum time length in seconds of trigger range to filter short time periods");
				triggerLevelCombo.setItems(new String[] { "250", "500", "1000", "1500", "2000", "2500", "3000", "3500", "4000", "5000", "7500"});
				triggerLevelCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "triggerLevelCombo.widgetSelected, event=" + evt);
						triggerLevel = triggerLevelCombo.getSelectionIndex() + 1;
						if (triggerType != null) {
							triggerType.setLevel(triggerLevel);
							deviceConfig.setChangePropery(true);
						}
					}
				});
				triggerLevelCombo.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "triggerLevelCombo.keyReleased, event=" + evt);
						triggerLevel = Integer.valueOf(triggerLevelCombo.getText());
						if (triggerType != null) {
							triggerType.setLevel(triggerLevel);
							deviceConfig.setChangePropery(true);
						}
					}
				});
				triggerLevelCombo.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(Level.FINEST, "triggerLevelCombo.verifyText, event=" + evt);
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				triggerCommentText = new Text(statisticsComposite, SWT.BORDER);
				triggerCommentText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				triggerCommentText.setBounds(360, 5, 325, 20);
				triggerCommentText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "triggerCommentText.keyReleased, event=" + evt);
						triggerComment = triggerCommentText.getText();
						if (triggerType != null) {
							triggerType.setComment(triggerComment);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				isGreaterButton = new Button(statisticsComposite, SWT.CHECK | SWT.LEFT);
				isGreaterButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				isGreaterButton.setText("isGreater");
				isGreaterButton.setBounds(125, 30, 75, 20);
				isGreaterButton.setToolTipText("true means all values above trigger level will be counted");
				isGreaterButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "isGreaterButton.widgetSelected, event=" + evt);
						isGreater = isGreaterButton.getSelection();
						if (triggerType != null) {
							triggerType.setIsGreater(isGreater);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				minTimeSecLabel = new Label(statisticsComposite, SWT.RIGHT);
				minTimeSecLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				minTimeSecLabel.setText("minTimeSec");
				minTimeSecLabel.setBounds(200, 32, 75, 20);
			}
			{
				minTimeSecCombo = new CCombo(statisticsComposite, SWT.BORDER);
				minTimeSecCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				minTimeSecCombo.setBounds(280, 30, 75, 20);
				minTimeSecCombo.setToolTipText("minimum time length in seconds of trigger range to filter short time periods");
				minTimeSecCombo.setItems(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" });
				minTimeSecCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "minTimeSecCombo.widgetSelected, event=" + evt);
						minTimeSec = minTimeSecCombo.getSelectionIndex() + 1;
						if (triggerType != null) {
							triggerType.setMinTimeSec(minTimeSec);
							deviceConfig.setChangePropery(true);
						}
					}
				});
				minTimeSecCombo.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "minTimeSecCombo.keyReleased, event=" + evt);
						minTimeSec = Integer.valueOf(minTimeSecCombo.getText());
						if (triggerType != null) {
							triggerType.setMinTimeSec(minTimeSec);
							deviceConfig.setChangePropery(true);
						}
					}
				});
				minTimeSecCombo.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(Level.FINEST, "minTimeSecCombo.verifyText, event=" + evt);
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				countByTriggerButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				countByTriggerButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				countByTriggerButton.setText("countByTrigger");
				countByTriggerButton.setBounds(125, 55, 170, 20);
				countByTriggerButton.setToolTipText("counts the number of events trigger level becomes active  at specified trigger type ");
				countByTriggerButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "countByTriggerButton.widgetSelected, event=" + evt);
						isCountByTrigger = countByTriggerButton.getSelection();
						countTriggerText.setEnabled(isCountByTrigger);
						if (statisticsType != null) {
							statisticsType.setCountByTrigger(isCountByTrigger);
							deviceConfig.setChangePropery(true);
							if (!isCountByTrigger) {
								statisticsType.setCountTriggerText(OSDE.STRING_EMPTY);
							}
						}
					}
				});
			}
			{
				countTriggerText = new Text(statisticsComposite, SWT.BORDER);
				countTriggerText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				countTriggerText.setBounds(360, 55, 325, 20);
				countTriggerText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "countTriggerText.keyReleased, event=" + evt);
						countTriggerComment = countTriggerText.getText();
						if (statisticsType != null) {
							statisticsType.setCountTriggerText(countTriggerComment);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				isTriggerRefOrdinalButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				isTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				isTriggerRefOrdinalButton.setText("triggerRef");
				isTriggerRefOrdinalButton.setBounds(125, 80, 150, 20);
				isTriggerRefOrdinalButton.setToolTipText("references the measurement ordinal where trigger level is set in case of trigger is defined (0=VoltageReceiver;1=Voltage,2=Current, ...)");
				isTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "isTriggerRefOrdinalButton.widgetSelected, event=" + evt);
						if (isTriggerRefOrdinalButton.getSelection()) {
							triggerRefOrdinalCombo.setEnabled(true);
							deviceConfig.setChangePropery(true);
						}
						else {
							triggerRefOrdinalCombo.setEnabled(false);
							if (statisticsType != null) {
								deviceConfig.setChangePropery(true);
								statisticsType.setTriggerRefOrdinal(triggerRefOrdinal = null);
							}
						}
					}
				});
			}
			{
				triggerRefOrdinalCombo = new CCombo(statisticsComposite, SWT.BORDER);
				triggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				triggerRefOrdinalCombo.setBounds(280, 80, 75, 20);
				triggerRefOrdinalCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				triggerRefOrdinalCombo.setEditable(false);
				triggerRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "triggerRefOrdinalCombo.widgetSelected, event=" + evt);
						triggerRefOrdinal = triggerRefOrdinalCombo.getSelectionIndex() + 1;
						if (statisticsType != null) {
							statisticsType.setTriggerRefOrdinal(triggerRefOrdinal);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				isSumByTriggerRefOrdinalButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				isSumByTriggerRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				isSumByTriggerRefOrdinalButton.setText("sumByTriggerRef");
				isSumByTriggerRefOrdinalButton.setBounds(125, 105, 150, 20);
				isSumByTriggerRefOrdinalButton.setToolTipText("calculates sum of values where trigger level becomes active at referenced triggered measurement");
				isSumByTriggerRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "isSumByTriggerRefOrdinalButton.widgetSelected, event=" + evt);
						if (isSumByTriggerRefOrdinalButton.getSelection()) {
							sumByTriggerRefOrdinalCombo.setEnabled(true);
							sumTriggerText.setEnabled(true);
						}
						else {
							sumByTriggerRefOrdinalCombo.setEnabled(false);
							sumTriggerText.setEnabled(false);
						}
						if(deviceConfig != null) deviceConfig.setChangePropery(true);
					}
				});
			}
			{
				sumByTriggerRefOrdinalCombo = new CCombo(statisticsComposite, SWT.BORDER);
				sumByTriggerRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				sumByTriggerRefOrdinalCombo.setBounds(280, 105, 75, 20);
				sumByTriggerRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "sumByTriggerRefOrdinalCombo.widgetSelected, event=" + evt);
						sumByTriggerRefOrdinal = sumByTriggerRefOrdinalCombo.getSelectionIndex();
						if (statisticsType != null) {
							statisticsType.setSumByTriggerRefOrdinal(sumByTriggerRefOrdinal);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				sumTriggerText = new Text(statisticsComposite, SWT.BORDER);
				sumTriggerText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				sumTriggerText.setBounds(360, 105, 325, 20);
				sumTriggerText.setToolTipText("this is the text displayed in front of sum value in case of trigger is defined");
				sumTriggerText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "sumTriggerText.keyReleased, event=" + evt);
						sumTriggerComment = sumTriggerText.getText();
						if (statisticsType != null) {
							statisticsType.setSumTriggerText(sumTriggerComment);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				isRatioRefOrdinalButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
				isRatioRefOrdinalButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				isRatioRefOrdinalButton.setText("ratioRef");
				isRatioRefOrdinalButton.setBounds(125, 130, 150, 20);
				isRatioRefOrdinalButton.setToolTipText("measurement ordinal to calculate the ratio of referenced avg or max value to sumByTriggerRef");
				isRatioRefOrdinalButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "isRatioRefOrdinalButton.widgetSelected, event=" + evt);
						isRatioRefOrdinal = isRatioRefOrdinalButton.getSelection();
						ratioRefOrdinalCombo.setEnabled(isRatioRefOrdinal);
						ratioRefOrdinalCombo.setItems(measurementReferenceItems);
						ratioText.setEnabled(isRatioRefOrdinal);
						if (statisticsType != null && !isRatioRefOrdinal) {
							statisticsType.setRatioRefOrdinal(null);
							ratioText.setText(OSDE.STRING_EMPTY);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				ratioRefOrdinalCombo = new CCombo(statisticsComposite, SWT.BORDER);
				ratioRefOrdinalCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				ratioRefOrdinalCombo.setBounds(280, 130, 75, 20);
				ratioRefOrdinalCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "ratioRefOrdinalCombo.widgetSelected, event=" + evt);
						ratioRefOrdinal = ratioRefOrdinalCombo.getSelectionIndex();
						if (statisticsType != null) {
							statisticsType.setRatioRefOrdinal(ratioRefOrdinal);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			{
				ratioText = new Text(statisticsComposite, SWT.BORDER);
				ratioText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
				ratioText.setBounds(360, 130, 325, 20);
				ratioText.setToolTipText("text displayed in front of the calculated ratio case of resolvable dependencyes");
				ratioText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "ratioText.keyReleased, event=" + evt);
						ratioComment = ratioText.getText();
						if (statisticsType != null) {
							statisticsType.setRatioText(ratioComment);
							deviceConfig.setChangePropery(true);
						}
					}
				});
			}
			scrolledComposite.setContent(statisticsComposite);
			statisticsComposite.setSize(700, 160);
			statisticsComposite.layout();

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
