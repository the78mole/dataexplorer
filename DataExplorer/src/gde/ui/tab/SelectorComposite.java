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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import gde.GDE;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.CurveSelectorContextMenu;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * This class defines a composite with a header (Curve Selector, ..) and a table with checkable table items
 * The table has a popup menu to manipulate properties of the items behind the table items
 * @author Winfried Brügmann
 */
public class SelectorComposite extends Composite {
  final static Logger           log                                 = Logger.getLogger(SelectorComposite.class.getName());

  final DataExplorer  						application = DataExplorer.getInstance();
  final Channels                	channels    = Channels.getInstance();
  final SashForm									parent;
  final int												windowType;
  final String                  	headerText;
  final Menu                    	popupmenu;
	final CurveSelectorContextMenu	contextMenu;
  
  int                           headerTextExtentFactor = 9;
	Button												curveSelectorHeader;
	int														initialSelectorHeaderWidth;
	int														selectorColumnWidth;
	Table													curveSelectorTable;
	TableColumn										tableSelectorColumn;
	
	int														oldSelectorColumnWidth = 0; 
	Point													oldSize = new Point(0,0);

	/**
	 * @param useParent
	 * @param useWindowType
	 */
	public SelectorComposite(final SashForm useParent, final int useWindowType, final String useHeaderText) {
		super(useParent, SWT.NONE);
		//this = new Composite(this.graphicSashForm, SWT.NONE);
		this.parent = useParent;
		this.windowType = useWindowType;
		this.headerText = useHeaderText;
		SWTResourceManager.registerResourceUser(this);
		
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new CurveSelectorContextMenu();
		this.contextMenu.createMenu(SelectorComposite.this.popupmenu);		
		
		initGUI();
	}

	void initGUI() {
		 // curveSelector
		
		FormLayout curveSelectorLayout = new FormLayout();
		this.setLayout(curveSelectorLayout);
		GridData curveSelectorLData = new GridData();
		this.setLayoutData(curveSelectorLData);
		this.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "curveSelector.helpRequested " + evt); //$NON-NLS-1$
				SelectorComposite.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
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
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "curveSelectorHeader.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (!curveSelectorHeader.getSelection()) {
						//use this check button to deselect all selected curves
						for (TableItem tableItem : curveSelectorTable.getItems()) {
							if (tableItem.getChecked()) toggleRecordSelection(tableItem, false, false);
						}
					}
					else {
						for (TableItem tableItem : curveSelectorTable.getItems()) {
							if (!tableItem.getChecked()) toggleRecordSelection(tableItem, false, true);
						}
					}
					doUpdateCurveSelectorTable();
					SelectorComposite.this.application.updateAllTabs(true, false);
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
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "curveSelectorTable.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (evt != null && evt.item != null) {
						toggleRecordSelection((TableItem) evt.item, true, false);
						SelectorComposite.this.application.updateAllTabs(true, false);
					}
				}
			});
			{
				synchronized (this) {
					this.tableSelectorColumn = new TableColumn(this.curveSelectorTable, SWT.LEAD);
					this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
				}
			}
		}	
	}

	/**
	 * executes the update of the curve selector table
	 */
	public synchronized void doUpdateCurveSelectorTable() {
		SelectorComposite.this.application.updateAllTabs(true, false);
		int itemWidth = this.initialSelectorHeaderWidth;
		RecordSet recordSet = null;
		switch (this.windowType) {
		case GraphicsWindow.TYPE_COMPARE:
			recordSet = this.application.getCompareSet();
			break;

		case GraphicsWindow.TYPE_UTIL:
			recordSet = this.application.getUtilitySet();
			break;

		case GraphicsWindow.TYPE_NORMAL:
		default:
			recordSet = this.channels.getActiveChannel() != null ? this.channels.getActiveChannel().getActiveRecordSet() : null;
			break;
		}
		if (recordSet != null && recordSet.size() > 0) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSet.getName());
				this.curveSelectorTable.removeAll();
				int checkBoxWidth = 20;
				int textSize = 10;
				boolean isOneVisible = false;
				for (int i=0; i<recordSet.size(); ++i) {
					Record record = recordSet.get(i);
					if (record != null) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName());
						textSize = record.getName().length() * 8;
						if (itemWidth < (textSize + checkBoxWidth)) itemWidth = textSize + checkBoxWidth;
						//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, item.getText() + " " + itemWidth);
						if (record.isDisplayable()) {
							TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
							item.setForeground(record.getColor());
							item.setText(record.getName());
							if (record.isVisible()) {
								isOneVisible = true;
								item.setChecked(true);
								item.setData(DataExplorer.OLD_STATE, true);
								item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
							}
							else {
								item.setChecked(false);
								item.setData(DataExplorer.OLD_STATE, false);
								item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
							}
							setHeaderSelection(isOneVisible);							
						}
					}
				}				
				this.selectorColumnWidth = itemWidth;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "*curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
				recordSet.updateVisibleAndDisplayableRecords();
		}
		else {
			this.curveSelectorTable.removeAll();
			this.selectorColumnWidth = this.initialSelectorHeaderWidth;
			setHeaderSelection(false);
		}
		
		if (this.oldSelectorColumnWidth != this.selectorColumnWidth) {
			this.curveSelectorHeader.setSize(this.selectorColumnWidth-1, this.curveSelectorHeader.getSize().y);
			this.tableSelectorColumn.setWidth(this.selectorColumnWidth-2);
			this.oldSelectorColumnWidth = this.selectorColumnWidth;
			this.application.setGraphicsSashFormWeights(this.selectorColumnWidth, this.windowType);
		}

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
	}

	/**
	 * @param enable
	 */
	public void setHeaderSelection(boolean enable) {
		curveSelectorHeader.setSelection(enable);
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
	 * toggles selection state of a record
	 * @param item table were selection need toggle state
	 */
	public void toggleRecordSelection(TableItem item, boolean isTableSelection, boolean forceVisible) {
		String recordName = item.getText();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "selected = " + recordName); //$NON-NLS-1$
		SelectorComposite.this.popupmenu.setData(DataExplorer.RECORD_NAME, recordName);
		SelectorComposite.this.popupmenu.setData(DataExplorer.CURVE_SELECTION_ITEM, item);
		if (!isTableSelection || item.getChecked() != (Boolean) item.getData(DataExplorer.OLD_STATE)) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "selection state changed = " + recordName); //$NON-NLS-1$
			Record activeRecord;
			switch (SelectorComposite.this.windowType) {
			case GraphicsWindow.TYPE_COMPARE:
				activeRecord = SelectorComposite.this.application.getCompareSet().getRecord(recordName);
				break;

			case GraphicsWindow.TYPE_UTIL:
				activeRecord = SelectorComposite.this.application.getUtilitySet().getRecord(recordName);
				break;

			default:
				activeRecord = SelectorComposite.this.channels.getActiveChannel().getActiveRecordSet().getRecord(recordName);
				break;
			}
			if (activeRecord != null) {
				activeRecord.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
				if (isTableSelection && item.getChecked() || forceVisible) {
					activeRecord.setVisible(true);
					SelectorComposite.this.popupmenu.getItem(0).setSelection(true);
					item.setData(DataExplorer.OLD_STATE, true);
					item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
					setHeaderSelection(true);
				}
				else {
					activeRecord.setVisible(false);
					SelectorComposite.this.popupmenu.getItem(0).setSelection(false);
					item.setData(DataExplorer.OLD_STATE, false);
					item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
				}
				activeRecord.getParent().syncScaleOfSyncableRecords();
				activeRecord.getParent().updateVisibleAndDisplayableRecords();
			}
		}
	}
}
