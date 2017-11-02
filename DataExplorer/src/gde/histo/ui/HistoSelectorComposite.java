/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2016,2017 Thomas Eickert
****************************************************************************************/
package gde.histo.ui;

import static java.util.logging.Level.FINEST;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import gde.GDE;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.CurveSelectorContextMenu;
import gde.ui.tab.GraphicsWindow.GraphicsType;

/**
 * Composite with a header (Curve Selector, ..) and table rows for the curves.
 * The first column holds checkable items and the 2nd one a curve type combobox.
 * The table has a popup menu to manipulate properties of the items behind the table items.
 * @author Thomas Eickert
 */
public final class HistoSelectorComposite extends Composite {
	private final static String			$CLASS_NAME							= HistoSelectorComposite.class.getName();
	private final static Logger			log											= Logger.getLogger($CLASS_NAME);

	private static final int				TEXT_EXTENT_FACTOR			= 6;

	private final DataExplorer			application							= DataExplorer.getInstance();
	private final Menu							popupmenu;
	final CurveSelectorContextMenu	contextMenu;

	private Button									curveSelectorHeader;
	private int											initialSelectorHeaderWidth;
	private int											selectorColumnWidth;
	private Table										curveSelectorTable;
	private TableColumn							tableSelectorColumn;
	private TableColumn							tableCurveTypeColumn;
	private int											initialCurveTypeColumnWidth;
	private int											curveTypeColumnWidth;

	private TableEditor[]						editors									= new TableEditor[0];

	private int											oldSelectorColumnWidth	= 0;

	/**
	 * @param useParent
	 */
	public HistoSelectorComposite(final SashForm useParent) {
		super(useParent, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new CurveSelectorContextMenu();
		this.contextMenu.createMenu(HistoSelectorComposite.this.popupmenu);

		initGUI();
	}

	void initGUI() {
		FormLayout curveSelectorLayout = new FormLayout();
		this.setLayout(curveSelectorLayout);
		GridData curveSelectorLData = new GridData();
		this.setLayoutData(curveSelectorLData);
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				log.log(FINEST, "helpRequested ", evt); //$NON-NLS-1$
				HistoSelectorComposite.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		{
			this.curveSelectorHeader = new Button(this, SWT.CHECK | SWT.LEFT);
			this.curveSelectorHeader.setText(Messages.getString(MessageIds.GDE_MSGT0254));
			this.curveSelectorHeader.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0671));
			this.curveSelectorHeader.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
			this.curveSelectorHeader.pack();
			this.initialSelectorHeaderWidth = this.curveSelectorHeader.getSize().x + 8;
			FormData curveSelectorHeaderLData = new FormData();
			curveSelectorHeaderLData.width = this.initialSelectorHeaderWidth;
			curveSelectorHeaderLData.height = 25;
			curveSelectorHeaderLData.left = new FormAttachment(0, 1000, GDE.IS_WINDOWS ? 6 : 0);
			curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 0);
			this.curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
			this.curveSelectorHeader.setBackground(DataExplorer.COLOR_LIGHT_GREY);
			this.curveSelectorHeader.setForeground(DataExplorer.COLOR_BLACK);
			this.curveSelectorHeader.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "curveSelectorHeader.widgetSelected, event=" + evt); //$NON-NLS-1$
					HistoSelectorComposite.this.application.clearMeasurementModes();
					if (!HistoSelectorComposite.this.curveSelectorHeader.getSelection()) {
						// use this check button to deselect all selected curves
						for (TableItem tableItem : HistoSelectorComposite.this.curveSelectorTable.getItems()) {
							if (tableItem.getChecked()) {
								// avoid phantom measurements with invisible curves
								final TrailRecord activeRecord = getTableItemRecord(tableItem);
								TrailRecordSet trailRecordSet = HistoSelectorComposite.this.application.getHistoSet().getTrailRecordSet();
								if (trailRecordSet.getRecordKeyMeasurement().equals(activeRecord.getName())) {
									HistoSelectorComposite.this.contextMenu.setMeasurement(activeRecord.getName(), false);
									HistoSelectorComposite.this.contextMenu.setDeltaMeasurement(activeRecord.getName(), false);
								}
								toggleRecordSelection(tableItem, false, false);
							}
						}
					} else {
						for (TableItem tableItem : HistoSelectorComposite.this.curveSelectorTable.getItems()) {
							if (!tableItem.getChecked()) {
								toggleRecordSelection(tableItem, false, true);
							}
						}
					}
					doUpdateCurveSelectorTable();
					HistoSelectorComposite.this.application.updateHistoGraphicsWindow(false);
				}
			});
		}
		{
			this.curveSelectorTable = new Table(this, SWT.FULL_SELECTION | SWT.SINGLE | SWT.CHECK);
			this.curveSelectorTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.curveSelectorTable.setLinesVisible(true);
			FormData curveTableLData = new FormData();
			curveTableLData.width = 82;
			curveTableLData.height = 457;
			curveTableLData.left = new FormAttachment(0, 1000, 0);
			curveTableLData.top = new FormAttachment(0, 1000, 25);
			curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
			curveTableLData.right = new FormAttachment(1000, 1000, 0);
			this.curveSelectorTable.setLayoutData(curveTableLData);
			this.curveSelectorTable.setMenu(this.popupmenu);
			this.curveSelectorTable.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.fine(() -> "curveSelectorTable.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (evt != null && evt.item != null) {
						final TableItem eventItem = (TableItem) evt.item;
						// avoid phantom measurements with invisible curves
						log.finer(() -> "checked/Old=" + eventItem.getChecked() + eventItem.getData(DataExplorer.OLD_STATE)); //$NON-NLS-1$
						final TrailRecord activeRecord = getTableItemRecord(eventItem);
						TrailRecordSet trailRecordSet = HistoSelectorComposite.this.application.getHistoSet().getTrailRecordSet();
						if (!eventItem.getChecked() && (Boolean) eventItem.getData(DataExplorer.OLD_STATE) //
								&& trailRecordSet.getRecordKeyMeasurement().equals(activeRecord.getName())) {
							HistoSelectorComposite.this.contextMenu.setMeasurement(activeRecord.getName(), false);
							HistoSelectorComposite.this.contextMenu.setDeltaMeasurement(activeRecord.getName(), false);
						}
						HistoSelectorComposite.this.popupmenu.setData(DataExplorer.RECORD_NAME, eventItem.getData(DataExplorer.RECORD_NAME));
						HistoSelectorComposite.this.popupmenu.setData(DataExplorer.CURVE_SELECTION_ITEM, eventItem);
						if (toggleRecordSelection(eventItem, true, false)) {
							HistoSelectorComposite.this.application.updateHistoGraphicsWindow(false);
						}
					}
				}
			});
			{
				synchronized (this) {
					this.tableSelectorColumn = new TableColumn(this.curveSelectorTable, SWT.LEAD);
					this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
					this.tableCurveTypeColumn = new TableColumn(this.curveSelectorTable, SWT.LEAD);
					this.tableCurveTypeColumn.setWidth(this.curveTypeColumnWidth);
				}
			}
		}
	}

	/**
	 * Update of the curve selector table.
	 */
	public synchronized void doUpdateCurveSelectorTable() {
		log.fine("start"); //$NON-NLS-1$
		this.curveSelectorTable.removeAll();
		for (TableEditor editor : this.editors) {
			if (editor != null) { // non displayable records
				if (editor.getEditor() != null) editor.getEditor().dispose();
				editor.dispose();
			}
		}
		int itemWidth = this.initialSelectorHeaderWidth;
		int checkBoxWidth = 20;
		int textSize = 10;
		int itemWidth2 = this.initialCurveTypeColumnWidth;
		int textSize2 = 10;
		boolean isOneVisible = false;
		TrailRecordSet recordSet = HistoSelectorComposite.this.application.getHistoSet().getTrailRecordSet();
		if (recordSet != null) {
			Combo[] selectorCombos = new Combo[recordSet.size()];
			this.editors = new TableEditor[recordSet.size()];
			for (int i = 0; i < recordSet.getDisplayRecords().size(); i++) {
				TrailRecord record = (TrailRecord) recordSet.getDisplayRecords().get(i);
				textSize = record.getName().length() * TEXT_EXTENT_FACTOR;
				if (itemWidth < textSize + checkBoxWidth) itemWidth = textSize + checkBoxWidth;
				textSize2 = (int) (record.getTrailSelector().getApplicableTrailsTexts().stream().mapToInt(w -> w.length()).max().orElse(10) * TEXT_EXTENT_FACTOR * 15 / 20.);
				if (itemWidth2 < textSize2 + checkBoxWidth) itemWidth2 = textSize2 + checkBoxWidth;
				// log.fine(() -> item.getText() + " " + itemWidth);
				if (record.isDisplayable()) {
					TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
					item.setForeground(record.getColor());
					item.setData(DataExplorer.RECORD_NAME, record.getName());
					item.setText(record.getNameReplacement().intern());

					this.editors[i] = new TableEditor(this.curveSelectorTable);
					selectorCombos[i] = new Combo(this.curveSelectorTable, SWT.READ_ONLY);
					this.editors[i].grabHorizontal = true;
					this.editors[i].setEditor(selectorCombos[i], item, 1);
					selectorCombos[i].setItems(record.getTrailSelector().getApplicableTrailsTexts().toArray(new String[0]));
					selectorCombos[i].setText(record.getTrailSelector().getTrailText());
					selectorCombos[i].setToolTipText(!record.getLabel().isEmpty() ? record.getLabel() : Messages.getString(MessageIds.GDE_MSGT0748));
					selectorCombos[i].addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							log.fine("selectorCombos.SelectionListener, event=" + event); //$NON-NLS-1$
							Combo combo = (Combo) event.getSource();
							record.getTrailSelector().setTrailTextSelectedIndex(combo.getSelectionIndex());
							HistoSelectorComposite.this.application.updateHistoTabs(record.getOrdinal(), true);
						}
					});
					if (record.isVisible()) {
						isOneVisible = true;
						item.setChecked(true);
						item.setData(DataExplorer.OLD_STATE, true);
					} else {
						item.setChecked(false);
						item.setData(DataExplorer.OLD_STATE, false);
					}
					setHeaderSelection(isOneVisible);
				}
			}
			this.selectorColumnWidth = itemWidth;
			this.curveTypeColumnWidth = itemWidth2;
			log.fine(() -> "curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
		}
		this.tableCurveTypeColumn.setWidth(this.curveTypeColumnWidth);
		if (this.oldSelectorColumnWidth != this.selectorColumnWidth) {
			this.curveSelectorHeader.setSize(this.selectorColumnWidth - 1, this.curveSelectorHeader.getSize().y);
			this.tableSelectorColumn.setWidth(this.selectorColumnWidth - 2);
			this.oldSelectorColumnWidth = this.selectorColumnWidth;
		}
		this.application.setGraphicsSashFormWeights(this.getCompositeWidth(), GraphicsType.HISTO);

		log.fine(() -> "curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
	}

	/**
	 * @param enable
	 */
	public void setHeaderSelection(boolean enable) {
		this.curveSelectorHeader.setSelection(enable);
	}

	/**
	 * @return the selectorColumnWidth
	 */
	public int getSelectorColumnWidth() {
		synchronized (this) {
			return this.selectorColumnWidth;
		}
	}

	/**
	 * @return the total width of all columns
	 */
	public int getCompositeWidth() {
		synchronized (this) {
			return this.selectorColumnWidth + this.curveTypeColumnWidth;
		}
	}

	public void setRecordSelection(TrailRecord activeRecord, boolean isVisible) {
		final TableItem tableItem = Arrays.stream(this.curveSelectorTable.getItems()).filter(c -> ((String) c.getData(DataExplorer.RECORD_NAME)).equals(activeRecord.getName())).findFirst().orElseThrow(UnsupportedOperationException::new);
		tableItem.setChecked(isVisible);
		if (activeRecord != null) setRecordSelection(activeRecord, isVisible, tableItem);
	}

	private void setRecordSelection(TrailRecord activeRecord, boolean isVisible, final TableItem tableItem) {
		// activeRecord.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		log.fine(() -> "isVisible old= " + activeRecord.isVisible()); //$NON-NLS-1$
		if (isVisible) {
			activeRecord.setVisible(true);
			// activeRecord.setDisplayable(true);
			HistoSelectorComposite.this.popupmenu.getItem(0).setSelection(true);
			tableItem.setData(DataExplorer.OLD_STATE, true);
			setHeaderSelection(true);
		} else {
			activeRecord.setVisible(false);
			// activeRecord.setDisplayable(false);
			HistoSelectorComposite.this.popupmenu.getItem(0).setSelection(false);
			tableItem.setData(DataExplorer.OLD_STATE, false);
		}
		activeRecord.getParentTrail().syncScaleOfSyncableRecords();
		activeRecord.getParentTrail().updateVisibleAndDisplayableRecordsForTable();
	}

	/**
	 * Toggle selection state of a record.
	 * @param item table were selection need toggle state
	 * @param isTableSelection
	 * @param forceVisible
	 * @return true if the state was actually changed
	 */
	private boolean toggleRecordSelection(TableItem item, boolean isTableSelection, boolean forceVisible) {
		boolean isToggled = false;
		TrailRecord activeRecord = getTableItemRecord(item);
		if (!isTableSelection || item.getChecked() != (Boolean) item.getData(DataExplorer.OLD_STATE)) {
			isToggled = true;
			log.fine(() -> "selection state changed= " + activeRecord.getName()); //$NON-NLS-1$
			// get newest timestamp and newest recordSet within this entry (both collections are in descending order)
			if (activeRecord != null) {
				setRecordSelection(activeRecord, isTableSelection && item.getChecked() || forceVisible, item);
				log.fine(() -> "isVisible= " + activeRecord.isVisible()); //$NON-NLS-1$
			}
		}
		return isToggled;
	}

	/**
	 * @param item
	 * @return the record of the record represented by the table item
	 */
	private TrailRecord getTableItemRecord(TableItem item) {
		TrailRecordSet trailRecordSet = HistoSelectorComposite.this.application.getHistoSet().getTrailRecordSet();
		return (TrailRecord) trailRecordSet.getRecord((String) item.getData(DataExplorer.RECORD_NAME));
	}
}
