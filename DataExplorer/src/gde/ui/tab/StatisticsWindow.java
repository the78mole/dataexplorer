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
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class StatisticsWindow {
	final static Logger						log						= Logger.getLogger(StatisticsWindow.class.getName());

	static final String	DELIMITER	= "!";
	static final String	NO_VALUE	= "---";

	CTabItem											statistics;
	Composite											composite;
	Composite											filler;
	Group									descriptionGroup, group0, group1;
	Group[]												measurementGroups;
	Text								textLabel;
	CLabel								minLabel;
	CLabel								maxLabel;
	CLabel								avgLabel;
	Table									dataTable;
	TableColumn						measurementTableColumn;
	TableColumn						unitTableColumn;
	TableColumn						sigmaTableColumn;
	TableColumn						customTableColumn;
	CLabel								sigmaLabel;
	CLabel								extraLabel;
	TableColumn						avgTableColumn;
	TableColumn						maxTableColumn;
	TableColumn						minTableColumn;
	
	// internal display variables
	String descriptionText = "";
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
		this.statistics.setText("Statistik"); //Messages.getString(MessageIds.OSDE_MSGT0233));
		SWTResourceManager.registerResourceUser(this.statistics);
		initGUI();
	}

	private void initGUI() {
		try {
			this.composite = new Composite(this.tabFolder, SWT.BORDER);
			this.statistics.setControl(this.composite);
			this.composite.setLayout(null);
			this.composite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.composite.addControlListener(new ControlListener() {
				public void controlResized(ControlEvent evt) {
					log.fine("composite.controlResized evt=" + evt);
					StatisticsWindow.this.descriptionGroup.setSize(StatisticsWindow.this.composite.getClientArea().width-20, 95);
					StatisticsWindow.this.textLabel.setSize(StatisticsWindow.this.descriptionGroup.getClientArea().width-15, StatisticsWindow.this.descriptionGroup.getClientArea().height-25);
					adaptTableSize();
				}
				public void controlMoved(ControlEvent evt) {
					log.finest("composite.controlMoved evt=" + evt);
				}
			});
			{
				this.descriptionGroup = new Group(this.composite, SWT.NONE);
				this.descriptionGroup.setLayout(null);
				this.descriptionGroup.setBounds(10, 5, 300, 95); // set top,left and maintain the rest by paint listener
				this.descriptionGroup.setText("Beschreibung");
				this.descriptionGroup.setBackground(SWTResourceManager.getColor(255, 255, 255));
				this.descriptionGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.fine("group0.paintControl, event=" + evt);
						updateStatisticsData();
						StatisticsWindow.this.textLabel.setText(StatisticsWindow.this.descriptionText);
					}
				});
				{
					this.textLabel = new Text(this.descriptionGroup, SWT.LEFT | SWT.MULTI );
					this.textLabel.setText("recordSetName, (fileDescription), recordSetDescription");
					this.textLabel.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.textLabel.setBounds(10, 20, this.descriptionGroup.getClientArea().width-15, this.descriptionGroup.getClientArea().height-25);
					this.textLabel.setEditable(false);
				}
			}
			{
				this.dataTable = new Table(this.composite, SWT.MULTI);
				this.dataTable.setLinesVisible(false);
				this.dataTable.setHeaderVisible(true);
				this.dataTable.setFont(SWTResourceManager.getFont(this.dataTable.getFont().getFontData()[0].getName(), 10, SWT.NORMAL));
				this.dataTable.setBounds(10, 150, 300, 100); // set top,left and maintain the rest by paint listener
//				this.dataTable.addPaintListener(new PaintListener() {
//					public void paintControl(PaintEvent evt) {
//						log.fine("dataTable.paintControl, event=" + evt);
//						updateStatisticsData();
//						//updateDataTable();
//					}
//				});
				{
					this.measurementTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.measurementTableColumn.setWidth(180);
					this.measurementTableColumn.setText("Messwert");
				}
				{
					this.unitTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.unitTableColumn.setWidth(120);
					this.unitTableColumn.setText("Einheit");
					this.unitTableColumn.setAlignment(SWT.LEFT);
				}
				{
					this.minTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.minTableColumn.setWidth(80);
					this.minTableColumn.setText("Minimum");
				}
				{
					this.avgTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.avgTableColumn.setWidth(80);
					this.avgTableColumn.setText("Mittelwert");
				}
				{
					this.maxTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.maxTableColumn.setWidth(80);
					this.maxTableColumn.setText("Maximum");
				}
				{
					this.sigmaTableColumn = new TableColumn(this.dataTable, SWT.CENTER);
					this.sigmaTableColumn.setText("Standardabweichung");
					this.sigmaTableColumn.setWidth(("Standardabweichung".length() * this.extentFactor > 80) ? "Standardabweichung".length() * this.extentFactor : 80);
				}
				{
					this.customTableColumn = new TableColumn(this.dataTable, SWT.LEFT);
					this.customTableColumn.setWidth(300);
					//this.customTableColumn.setText("Custom");
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
					StatisticsWindow.this.descriptionText = StatisticsWindow.this.channels.getFileDescription() + "\n\n" + activeRecordSet.getRecordSetDescription();
					StatisticsWindow.this.descriptionGroup.redraw();
					try {
						String[] displayableRecords = activeRecordSet.getDisplayableRecordNames();
						this.oldNumberDisplayableRecords = displayableRecords.length;

						StringBuilder sb = new StringBuilder();
						this.tabelItemText = new Vector<String>();

						// time
						String time = Messages.getString(MessageIds.OSDE_MSGT0234);
						sb.append(time.split(" ")[0]).append(DELIMITER);
						sb.append("[HH:mm:ss:SSS]").append(DELIMITER);
						sb.append("0").append(DELIMITER);
						sb.append(NO_VALUE).append(DELIMITER);
						sb.append(TimeLine.getFomatedTime(activeRecordSet.getTimeStep_ms() * activeRecordSet.getRecordDataSize(true))).append(" ").append(DELIMITER);
						sb.append(NO_VALUE).append(DELIMITER);
						sb.append("Interwall = ").append(String.format("%6.1f", activeRecordSet.getTimeStep_ms())).append(" ms ").append(DELIMITER);
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
								sb.append("[").append(record.getUnit()).append("]").append(DELIMITER);

								if (measurementStatistics.isMin()) {
									if (isTriggerLevel)
										sb.append(df.format(record.getMinValueTriggered() / 1000.0));
									else
										sb.append(df.format((triggerRefOrdinal < 0 ? record.getMinValue() : record.getMinValueTriggered(triggerRefOrdinal)) / 1000.0));
								}
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isAvg())
									if (isTriggerLevel)
										sb.append(df.format(record.getAvgValueTriggered() / 1000.0));
									else
										sb.append(df.format((triggerRefOrdinal < 0 ? record.getAvgValue() : record.getAvgValueTriggered(triggerRefOrdinal)) / 1000.0));
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isMax()) {
									if (isTriggerLevel)
										sb.append(df.format(record.getMaxValueTriggered() / 1000.0));
									else
										sb.append(df.format((triggerRefOrdinal < 0 ? record.getMaxValue() : record.getMaxValueTriggered(triggerRefOrdinal)) / 1000.0));
								}
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isSigma()) {
									if (isTriggerLevel)
										sb.append(df.format(record.getSigmaValueTriggered() / 1000.0));
									else
										sb.append(df.format((triggerRefOrdinal < 0 ? record.getSigmaValue() : record.getSigmaValueTriggered(triggerRefOrdinal)) / 1000.0));
								}
								else
									sb.append(NO_VALUE);
								sb.append(DELIMITER);

								if (measurementStatistics.isCountByTrigger() != null) sb.append(measurementStatistics.getCountTriggerText()).append(" = ").append(record.getTriggerRanges().size());

								if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
									if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1) {
										sb.append(measurementStatistics.getSumTriggerText()).append(" = ");
									}
									else {
										sb.append("Summe min/max Delta aus Triggerbereich").append(" = ");
									}
									if (isTriggerLevel)
										sb.append(df.format(record.getSumTriggeredRange() / 1000.0));
									else {
										if (measurementStatistics.getSumByTriggerRefOrdinal() != null)
											sb.append(df.format(record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.0));
										sb.append(" [").append(record.getUnit()).append("] ");
									}
								}
								if (measurementStatistics.getComment() != null && measurementStatistics.getComment().length() > 1) sb.append(" (").append(measurementStatistics.getComment()).append(")");

								if (measurementStatistics.getTrigger() != null && measurementStatistics.getTrigger().getComment() != null && measurementStatistics.getTrigger().getComment().length() > 1)
									sb.append(" (").append(measurementStatistics.getTrigger().getComment()).append(")");

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
				StatisticsWindow.this.descriptionText = "";
				this.tabelItemText = new Vector<String>();
				this.oldRecordSet = null;
			}
		}
		else {
			StatisticsWindow.this.descriptionText = "";
			this.tabelItemText = new Vector<String>();
			this.oldRecordSet = null;
		}
		updateDataTable();
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
			log.fine("ColumWidth = " + this.dataTable.getColumn(i).getWidth());
		}
		int tableHeight = this.dataTable.getItemCount() * this.dataTable.getItemHeight() + this.dataTable.getHeaderHeight();
		tableHeight = tableHeight+150 < this.composite.getClientArea().height ? tableHeight : this.composite.getClientArea().height-150;
		this.dataTable.setSize(StatisticsWindow.this.composite.getClientArea().width-20, tableHeight);
		this.customTableColumn.setWidth(this.dataTable.getClientArea().width - columsWidth);
	}

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
	//		StatisticsWindow inst = new StatisticsWindow(shell, SWT.NULL);
	//		Point size = inst.getSize();
	//		shell.setLayout(new FillLayout());
	//		shell.layout();
	//		if(size.x == 0 && size.y == 0) {
	//			inst.pack();
	//			shell.pack();
	//		} else {
	//			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
	//			shell.setSize(shellBounds.width, shellBounds.height);
	//		}
	//		shell.open();
	//		while (!shell.isDisposed()) {
	//			if (!display.readAndDispatch())
	//				display.sleep();
	//		}
	//	}
}
