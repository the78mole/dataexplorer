package osde.ui.tab;

import java.text.DecimalFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.StatisticsType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.TimeLine;

/**
 * class to represent statistics data according configuration in device properties XML file
 */
public class StatisticsWindow {
	final static Logger						log						= Logger.getLogger(StatisticsWindow.class.getName());

	static final String	DELIMITER	= "!"; //$NON-NLS-1$
	static final String	NO_VALUE	= "---"; //$NON-NLS-1$

	CTabItem											statistics;
	Composite											composite;
	Composite											filler;
	Group													descriptionGroup;
	Text													textLabel;
	CLabel												minLabel;
	CLabel												maxLabel;
	CLabel												avgLabel;
	Table													dataTable;
	TableColumn										measurementTableColumn;
	TableColumn										unitTableColumn;
	TableColumn										sigmaTableColumn;
	TableColumn										customTableColumn;
	CLabel												sigmaLabel;
	CLabel												extraLabel;
	TableColumn										avgTableColumn;
	TableColumn										maxTableColumn;
	TableColumn										minTableColumn;
	
	// internal display variables
	String descriptionText = ""; //$NON-NLS-1$
	Vector<String> tabelItemText = new Vector<String>();

	final int											extentFactor	= 9;																									// factor to calculate column width
	RecordSet											oldRecordSet	= null;
	int														oldNumberDisplayableRecords = 0;
	final OpenSerialDataExplorer	application;
	final Channels								channels;
	final CTabFolder							tabFolder;

	public StatisticsWindow(OpenSerialDataExplorer currentApplication, CTabFolder dataTab) {
		this.application = currentApplication;
		this.tabFolder = dataTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.statistics = new CTabItem(this.tabFolder, SWT.NONE);
		this.statistics.setText(Messages.getString(MessageIds.OSDE_MSGT0350));
		SWTResourceManager.registerResourceUser(this.statistics);
		initGUI();
	}

	private void initGUI() {
		try {
			this.composite = new Composite(this.tabFolder, SWT.NONE);
			this.statistics.setControl(this.composite);
			this.composite.setLayout(null);
			this.composite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.composite.addControlListener(new ControlListener() {
				public void controlResized(ControlEvent evt) {
					log.fine("composite.controlResized evt=" + evt); //$NON-NLS-1$
					StatisticsWindow.this.descriptionGroup.setSize(StatisticsWindow.this.composite.getClientArea().width-20, 110);
					StatisticsWindow.this.textLabel.setSize(StatisticsWindow.this.descriptionGroup.getClientArea().width-15, StatisticsWindow.this.descriptionGroup.getClientArea().height-10);
					adaptTableSize();
				}
				public void controlMoved(ControlEvent evt) {
					log.finest("composite.controlMoved evt=" + evt); //$NON-NLS-1$
				}
			});
			{
				this.descriptionGroup = new Group(this.composite, SWT.NONE);
				this.descriptionGroup.setLayout(null);
				this.descriptionGroup.setBounds(10, 10, 300, 110); // set top,left and maintain the rest by control listener
				this.descriptionGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0351));
				this.descriptionGroup.setBackground(SWTResourceManager.getColor(255, 255, 255));
				this.descriptionGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.fine("group0.paintControl, event=" + evt); //$NON-NLS-1$
						StatisticsWindow.this.textLabel.setText(StatisticsWindow.this.descriptionText);
					}
				});
				{
					this.textLabel = new Text(this.descriptionGroup, SWT.LEFT | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
					this.textLabel.setText("recordSetName, (fileDescription), recordSetDescription"); //$NON-NLS-1$
					this.textLabel.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.textLabel.setBounds(10, 20, this.descriptionGroup.getClientArea().width-15, this.descriptionGroup.getClientArea().height-10);
					this.textLabel.setEditable(false);
				}
			}
			{
				this.dataTable = new Table(this.composite, SWT.MULTI | SWT.BORDER);
				this.dataTable.setLinesVisible(false);
				this.dataTable.setHeaderVisible(true);
				this.dataTable.setFont(SWTResourceManager.getFont(this.dataTable.getFont().getFontData()[0].getName(), 10, SWT.NORMAL));
				this.dataTable.setBounds(10, 150, 300, 100); // set top,left and maintain the rest by control listener
				{
					this.measurementTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.measurementTableColumn.setWidth(180);
					this.measurementTableColumn.setText(Messages.getString(MessageIds.OSDE_MSGT0352));
				}
				{
					this.unitTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.unitTableColumn.setWidth(120);
					this.unitTableColumn.setText(Messages.getString(MessageIds.OSDE_MSGT0353));
					this.unitTableColumn.setAlignment(SWT.LEFT);
				}
				{
					this.minTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.minTableColumn.setWidth(90);
					this.minTableColumn.setText(Messages.getString(MessageIds.OSDE_MSGT0354));
				}
				{
					this.avgTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.avgTableColumn.setWidth(90);
					this.avgTableColumn.setText(Messages.getString(MessageIds.OSDE_MSGT0355));
				}
				{
					this.maxTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.maxTableColumn.setWidth(90);
					this.maxTableColumn.setText(Messages.getString(MessageIds.OSDE_MSGT0356));
				}
				{
					this.sigmaTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					String sigmaText = Messages.getString(MessageIds.OSDE_MSGT0357);
					this.sigmaTableColumn.setText(sigmaText);
					this.sigmaTableColumn.setWidth((sigmaText.length() * this.extentFactor > 90) ? sigmaText.length() * this.extentFactor : 80);
				}
				{
					this.customTableColumn = new TableColumn(this.dataTable, SWT.LEFT);
					this.customTableColumn.setWidth(300);
					this.customTableColumn.setText(Messages.getString(MessageIds.OSDE_MSGT0358));
				}
			}
			this.composite.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * update statistics window display data
	 */
	public void updateStatisticsData() {
		if (StatisticsWindow.this.channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null ) {
				if (activeRecordSet != this.oldRecordSet || activeRecordSet.getNumberOfDisplayableRecords() != this.oldNumberDisplayableRecords) {
					StatisticsWindow.this.descriptionText = StatisticsWindow.this.channels.getFileDescription() + "\n--------------------------\n"  //$NON-NLS-1$
						+ activeRecordSet.getName() + " :  " + activeRecordSet.getRecordSetDescription(); //$NON-NLS-1$
					try {
						String[] displayableRecords = activeRecordSet.getDisplayableRecordNames();
						this.oldNumberDisplayableRecords = displayableRecords.length;

						StringBuilder sb = new StringBuilder();
						this.tabelItemText = new Vector<String>();

						// time
						String time = Messages.getString(MessageIds.OSDE_MSGT0234);
						sb.append(time.split(" ")[0]).append(DELIMITER); //$NON-NLS-1$
						sb.append(Messages.getString(MessageIds.OSDE_MSGT0359)).append(DELIMITER);
						sb.append("0").append(DELIMITER); //$NON-NLS-1$
						sb.append(NO_VALUE).append(DELIMITER);
						sb.append(TimeLine.getFomatedTime(activeRecordSet.getTimeStep_ms() * activeRecordSet.getRecordDataSize(true))).append(" ").append(DELIMITER); //$NON-NLS-1$
						sb.append(NO_VALUE).append(DELIMITER);
						sb.append(Messages.getString(MessageIds.OSDE_MSGT0360)).append(String.format("%6.1f", activeRecordSet.getTimeStep_ms())).append(Messages.getString(MessageIds.OSDE_MSGT0361)).append(DELIMITER); //$NON-NLS-1$
						this.tabelItemText.add(sb.toString());

						for (String recordName : displayableRecords) {
							Record record = activeRecordSet.get(recordName);
							DecimalFormat df = record.getDecimalFormat();
							StatisticsType measurementStatistics = activeRecordSet.getDevice().getMeasurementStatistic(record.getChannelConfigKey(), activeRecordSet.getRecordNameIndex(recordName));
							if (measurementStatistics != null) {
								sb = new StringBuilder();
								int triggerRefOrdinal = measurementStatistics.getTriggerRefOrdinal() == null ? -1 : measurementStatistics.getTriggerRefOrdinal().intValue();
								boolean isTriggerLevel = measurementStatistics.getTrigger() != null;
								sb.append(record.getName()).append(DELIMITER);
								sb.append("[").append(record.getUnit()).append("]").append(DELIMITER); //$NON-NLS-1$ //$NON-NLS-2$

								if (measurementStatistics.isMin()) {
									if (isTriggerLevel)
										sb.append(formatOutput(df.format(record.getMinValueTriggered() / 1000.0)));
									else
										sb.append(formatOutput(df.format((triggerRefOrdinal < 0 ? record.getRealMinValue() : record.getMinValueTriggered(triggerRefOrdinal)) / 1000.0)));
								}
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isAvg())
									if (isTriggerLevel)
										sb.append(formatOutput(df.format(record.getAvgValueTriggered() / 1000.0)));
									else
										sb.append(formatOutput(df.format((triggerRefOrdinal < 0 ? record.getAvgValue() : record.getAvgValueTriggered(triggerRefOrdinal)) / 1000.0)));
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isMax()) {
									if (isTriggerLevel)
										sb.append(formatOutput(df.format(record.getMaxValueTriggered() / 1000.0)));
									else
										sb.append(formatOutput(df.format((triggerRefOrdinal < 0 ? record.getRealMaxValue() : record.getMaxValueTriggered(triggerRefOrdinal)) / 1000.0)));
								}
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isSigma()) {
									DecimalFormat cdf = new DecimalFormat("0.000"); //$NON-NLS-1$
									if (isTriggerLevel) {
										sb.append(formatOutput(cdf.format(record.getSigmaValueTriggered() / 1000.0)));
									}
									else
										sb.append(formatOutput(cdf.format((triggerRefOrdinal < 0 ? record.getSigmaValue() : record.getSigmaValueTriggered(triggerRefOrdinal)) / 1000.0)));
								}
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isCountByTrigger() != null) sb.append(measurementStatistics.getCountTriggerText()).append(" = ").append(record.getTriggerRanges().size()); //$NON-NLS-1$

								if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
									if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1) {
										sb.append(measurementStatistics.getSumTriggerText()).append(" = "); //$NON-NLS-1$
									}
									else {
										sb.append(measurementStatistics.getSumTriggerText()).append(" = "); //$NON-NLS-1$
									}
									if (isTriggerLevel)
										sb.append(df.format(record.getSumTriggeredRange() / 1000.0));
									else {
										if (measurementStatistics.getSumByTriggerRefOrdinal() != null)
											sb.append(df.format(record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.0));
										sb.append(" [").append(record.getUnit()).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
									}
								}
								if (measurementStatistics.getComment() != null && measurementStatistics.getComment().length() > 1) sb.append(" (").append(measurementStatistics.getComment()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$

								if (measurementStatistics.getTrigger() != null && measurementStatistics.getTrigger().getComment() != null && measurementStatistics.getTrigger().getComment().length() > 1)
									sb.append(" (").append(measurementStatistics.getTrigger().getComment()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$

								log.finer(sb.toString());
								this.tabelItemText.add(sb.toString());
							}
						}
					}
					catch (RuntimeException e) {
						StatisticsWindow.log.log(Level.WARNING, e.getMessage(), e);
					}
					this.oldRecordSet = activeRecordSet;
				}
			}
			else {
				StatisticsWindow.this.descriptionText = ""; //$NON-NLS-1$
				this.tabelItemText = new Vector<String>();
				this.oldRecordSet = null;
			}
		}
		else {
			StatisticsWindow.this.descriptionText = ""; //$NON-NLS-1$
			this.tabelItemText = new Vector<String>();
			this.oldRecordSet = null;
		}
		StatisticsWindow.this.descriptionGroup.redraw();
		updateDataTable();
	}

	/**
	 * format value string
	 * @param inDecimalString
	 */
	String formatOutput(String inDecimalString) {
		String[] tmp = inDecimalString.replace('.', ';').replace(',', ';').split(";"); //$NON-NLS-1$
		return tmp.length>1 ? String.format("%6s.%-5s", tmp[0], tmp[1]) : String.format("%6s%-6s", tmp[0], " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * method to update the data table
	 */
	void updateDataTable() {

		if (StatisticsWindow.log.isLoggable(Level.FINE)) StatisticsWindow.log.fine("entry data table update"); //$NON-NLS-1$

		// cleanup old data table
		this.dataTable.removeAll();

		// set items (rows) of data table
		TableItem row;

		for (String itemsText : this.tabelItemText) {
			this.dataTable.setItemCount(this.dataTable.getItemCount() + 1);
			row = new TableItem(this.dataTable, SWT.NONE);
			row.setText(itemsText.split(DELIMITER));
		}
		this.dataTable.setItemCount(this.dataTable.getItemCount() + 1);
		adaptTableSize();

		if (StatisticsWindow.log.isLoggable(Level.FINE)) StatisticsWindow.log.fine("exit data table update"); //$NON-NLS-1$
	}

	/**
	 * adapt the size of the data table
	 */
	void adaptTableSize() {
		// adapt custom part width
		int columsWidth = 0;
		for (int i = 0; i < this.dataTable.getColumnCount() - 1; i++) {
			columsWidth += this.dataTable.getColumn(i).getWidth();
			log.fine("ColumWidth = " + this.dataTable.getColumn(i).getWidth()); //$NON-NLS-1$
		}
		int tableHeight = this.dataTable.getItemCount() * this.dataTable.getItemHeight() + this.dataTable.getHeaderHeight() + 4;
		tableHeight = tableHeight+150 < this.composite.getClientArea().height ? tableHeight : this.composite.getClientArea().height-150;
		this.dataTable.setSize(StatisticsWindow.this.composite.getClientArea().width-20, tableHeight);
		this.customTableColumn.setWidth(this.dataTable.getClientArea().width - columsWidth);
	}
}
