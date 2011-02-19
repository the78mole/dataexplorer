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
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

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

/**
 * This class defines a composite whith a header (Curve Selector, ..) and a table with checkable table items
 * The table has a popup menu to manipulate properies of the items behind the table items
 * @author Winfried Brügmann
 */
public class SelectorComposite extends Composite {
  final static Logger           log                                 = Logger.getLogger(SelectorComposite.class.getName());

  final DataExplorer  	application = DataExplorer.getInstance();
  final Channels                	channels    = Channels.getInstance();
  final SashForm									parent;
  final int												windowType;
  final String                  	headerText;
  final Menu                    	popupmenu;
	final CurveSelectorContextMenu	contextMenu;
  
  int                           headerTextExtentFactor = 9;
	CLabel												curveSelectorHeader;
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
		this.addPaintListener(new PaintListener() {		
			public void paintControl(PaintEvent arg0) {
				doUpdateCurveSelectorTable();
			}
		});
		this.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "curveSelector.helpRequested " + evt); //$NON-NLS-1$
				SelectorComposite.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		{
			this.curveSelectorHeader = new CLabel(this, SWT.NONE);
			this.curveSelectorHeader.setText("  " + Messages.getString(MessageIds.GDE_MSGT0254)); //$NON-NLS-1$
			this.curveSelectorHeader.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
			this.curveSelectorHeader.pack();
			this.initialSelectorHeaderWidth = this.curveSelectorHeader.getSize().x + 8;
			FormData curveSelectorHeaderLData = new FormData();
			curveSelectorHeaderLData.width = this.initialSelectorHeaderWidth;
			curveSelectorHeaderLData.height = 25;
			curveSelectorHeaderLData.left = new FormAttachment(0, 1000, 0);
			curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 0);
			this.curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
			this.curveSelectorHeader.setBackground(DataExplorer.COLOR_LIGHT_GREY);
		}
		{
			this.curveSelectorTable = new Table(this, SWT.FULL_SELECTION | SWT.CHECK);
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
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "curveSelectorTable.widgetSelected, event=" + evt); //$NON-NLS-1$
						TableItem item = (TableItem) evt.item;
						String recordName = item.getText();
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "selected = " + recordName); //$NON-NLS-1$
						SelectorComposite.this.popupmenu.setData(DataExplorer.RECORD_NAME, recordName);
						SelectorComposite.this.popupmenu.setData(DataExplorer.CURVE_SELECTION_ITEM, evt.item);
						if (item.getChecked() != (Boolean) item.getData(DataExplorer.OLD_STATE)) {
							Record activeRecord;
							switch (SelectorComposite.this.windowType) {
							case GraphicsWindow.TYPE_COMPARE:
								activeRecord = SelectorComposite.this.application.getCompareSet().getRecord(recordName);
								break;

							default:
								activeRecord = SelectorComposite.this.channels.getActiveChannel().getActiveRecordSet().getRecord(recordName);
								break;
							}
							if (activeRecord != null) {
								activeRecord.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
								if (item.getChecked()) {
									activeRecord.setVisible(true);
									SelectorComposite.this.popupmenu.getItem(0).setSelection(true);
									item.setData(DataExplorer.OLD_STATE, true);
									item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
								}
								else {
									activeRecord.setVisible(false);
									SelectorComposite.this.popupmenu.getItem(0).setSelection(false);
									item.setData(DataExplorer.OLD_STATE, false);
									item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
								}
								activeRecord.getParent().syncScaleOfSyncableRecords();
							}
							SelectorComposite.this.application.updateAllTabs(false);
						}
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
		int itemWidth = this.initialSelectorHeaderWidth;
		RecordSet activeRecordSet = this.channels.getActiveChannel() != null ? this.channels.getActiveChannel().getActiveRecordSet() : null;
		RecordSet recordSet = this.windowType == GraphicsWindow.TYPE_NORMAL ? activeRecordSet : this.application.getCompareSet();
		if (recordSet != null && recordSet.size() > 0) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSet.getName());
				this.curveSelectorTable.removeAll();
				String[] recordKeys = recordSet.getRecordNames();
				int checkBoxWidth = 20;
				int textSize = 10;
				for (String recordKey : recordKeys) {
					Record record = recordSet.getRecord(recordKey);
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
								item.setChecked(true);
								item.setData(DataExplorer.OLD_STATE, true);
								item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
							}
							else {
								item.setChecked(false);
								item.setData(DataExplorer.OLD_STATE, false);
								item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
							}
						}
					}
				}				
				this.selectorColumnWidth = itemWidth;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "*curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
		}
		else {
			this.curveSelectorTable.removeAll();
			this.selectorColumnWidth = this.initialSelectorHeaderWidth;
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
	 * @return the selectorColumnWidth
	 */
	public int getSelectorColumnWidth() {
		synchronized (this) {
			return this.selectorColumnWidth;
		}
	}
}
