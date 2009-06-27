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
package osde.ui.tab;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * This class defines a composite whith a header (Curve Selector, ..) and a table with checkable table items
 * The table has a popup menu to manipulate properies of the items behind the table items
 * @author Winfried Brügmann
 */
public class SelectorComposite extends Composite {
  final static Logger           log                                 = Logger.getLogger(SelectorComposite.class.getName());

  final OpenSerialDataExplorer  application = OpenSerialDataExplorer.getInstance();
  final Channels                channels       = Channels.getInstance();
  final SashForm								parent;
  final int											windowType;
  final String                  headerText;
  final Menu                    popupmenu;
  
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
	public SelectorComposite(final SashForm useParent, final int useWindowType, final String useHeaderText, final Menu usePopUpMenu) {
		super(useParent, SWT.NONE);
		//this = new Composite(this.graphicSashForm, SWT.NONE);
		this.parent = useParent;
		this.windowType = useWindowType;
		this.headerText = useHeaderText;
		this.popupmenu = usePopUpMenu;
		SWTResourceManager.registerResourceUser(this);
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
				log.log(Level.FINER, "curveSelector.helpRequested " + evt); //$NON-NLS-1$
				SelectorComposite.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		{
			this.curveSelectorHeader = new CLabel(this, SWT.NONE);
			this.curveSelectorHeader.setText("  " + Messages.getString(MessageIds.OSDE_MSGT0254)); //$NON-NLS-1$
			this.curveSelectorHeader.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.BOLD));
			this.curveSelectorHeader.pack();
			this.initialSelectorHeaderWidth = this.curveSelectorHeader.getSize().x + 8;
			FormData curveSelectorHeaderLData = new FormData();
			curveSelectorHeaderLData.width = this.initialSelectorHeaderWidth;
			curveSelectorHeaderLData.height = 25;
			curveSelectorHeaderLData.left = new FormAttachment(0, 1000, 0);
			curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 0);
			this.curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
			this.curveSelectorHeader.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
		}
		{
			this.curveSelectorTable = new Table(this, SWT.SINGLE | SWT.CHECK | SWT.EMBEDDED);
			this.curveSelectorTable.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
			this.curveSelectorTable.setLinesVisible(true);
			FormData curveTableLData = new FormData();
			curveTableLData.width = 82;
			curveTableLData.height = 457;
			curveTableLData.left = new FormAttachment(0, 1000, 0);
			curveTableLData.top = new FormAttachment(0, 1000, 25);
			curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
			curveTableLData.right = new FormAttachment(1000, 1000, 0);
			this.curveSelectorTable.setLayoutData(curveTableLData);
			if (this.popupmenu != null) this.curveSelectorTable.setMenu(this.popupmenu);
			this.curveSelectorTable.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "curveTable.widgetSelected, event=" + evt); //$NON-NLS-1$
					TableItem item = (TableItem) evt.item;
					String recordName = ((TableItem) evt.item).getText();
					log.log(Level.FINE, "selected = " + recordName); //$NON-NLS-1$
					SelectorComposite.this.popupmenu.setData(OpenSerialDataExplorer.RECORD_NAME, recordName);
					SelectorComposite.this.popupmenu.setData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM, evt.item);
					if (item.getChecked() != (Boolean) item.getData(OpenSerialDataExplorer.OLD_STATE)) {
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
								item.setData(OpenSerialDataExplorer.OLD_STATE, true);
								item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
							}
							else {
								activeRecord.setVisible(false);
								SelectorComposite.this.popupmenu.getItem(0).setSelection(false);
								item.setData(OpenSerialDataExplorer.OLD_STATE, false);
								item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
							}
						}
						else {
							log.log(Level.FINER, "GraphicsWindow.type = " + SelectorComposite.this.windowType + " recordName = \"" + recordName + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							RecordSet activeRecordSet = SelectorComposite.this.channels.getActiveChannel() != null ? SelectorComposite.this.channels.getActiveChannel().getActiveRecordSet() : null;
							RecordSet recordSet = SelectorComposite.this.windowType == GraphicsWindow.TYPE_NORMAL ? activeRecordSet : SelectorComposite.this.application.getCompareSet();
							if (recordSet != null && recordSet.size() > 0) {
								if (item.getChecked()) {
									item.setData(OpenSerialDataExplorer.OLD_STATE, true);
									recordSet.setSyncRequested(true);
									//item.setChecked(recordSet.isSyncableSynced()); // only allow check if at least one of the syncable records are visible
								}
								else {
									item.setData(OpenSerialDataExplorer.OLD_STATE, false);
									recordSet.setSyncRequested(false);
								}
							}
						}
						SelectorComposite.this.application.updateGraphicsWindow();
						SelectorComposite.this.application.updateDigitalWindow();
						SelectorComposite.this.application.updateAnalogWindow();
						SelectorComposite.this.application.updateCellVoltageWindow();
						SelectorComposite.this.application.updateFileCommentWindow();
					}
				}
			});
			{
				this.tableSelectorColumn = new TableColumn(this.curveSelectorTable, SWT.LEFT);
				this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
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
				log.log(Level.FINE, recordSet.getName());
				this.curveSelectorTable.removeAll();
				//this.curveSelectorHeader.pack(true);
				//itemWidth = this.selectorHeaderWidth = this.curveSelectorHeader.getSize().x;
				String[] recordKeys = recordSet.getRecordNames();
				int checkBoxWidth = 20;
				int textSize = 10;
				for (int i = 0; i < recordSet.size(); i++) {
					Record record;
					switch (this.windowType) {
					case GraphicsWindow.TYPE_COMPARE:
						String recordKey = recordSet.getFirstRecordName().split("_")[0]; //$NON-NLS-1$
						record = recordSet.getRecord(recordKey + "_" + i); //$NON-NLS-1$
						break;

					default: // TYPE_NORMAL
						record = recordSet.getRecord(recordKeys[i]);
						break;
					}
					if (record != null) {
						log.log(Level.FINER, record.getName());
						textSize = record.getName().length() * 8;
						if (itemWidth < (textSize + checkBoxWidth)) itemWidth = textSize + checkBoxWidth;
						//log.log(Level.FINE, item.getText() + " " + itemWidth);
						//item.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.jpg"));
						if (record.isDisplayable()) {
							TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
							item.setForeground(record.getColor());
							item.setText(record.getName());
							if (record.isVisible()) {
								item.setChecked(true);
								item.setData(OpenSerialDataExplorer.OLD_STATE, true);
								item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
							}
							else {
								item.setChecked(false);
								item.setData(OpenSerialDataExplorer.OLD_STATE, false);
								item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
							}
						}
					}
				}
				if (recordSet.isSyncableDisplayableRecords(false) && recordSet.isOneSyncableVisible() && this.windowType == GraphicsWindow.TYPE_NORMAL) {
					TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
					item.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
					item.setText("Sync " + recordSet.getSyncableName());
					item.setChecked(recordSet.isSyncRequested());
					item.setData(OpenSerialDataExplorer.OLD_STATE, recordSet.isSyncRequested());
					item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
					item.setData(OpenSerialDataExplorer.RECORD_SYNC_PLACEHOLDER, true);
					textSize = item.getText().length() * 7;
					if (itemWidth < (textSize+checkBoxWidth)) itemWidth = textSize+checkBoxWidth;
				}
				
				this.selectorColumnWidth = itemWidth;
				log.log(Level.FINE, "*curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
		}
		else {
			this.curveSelectorTable.removeAll();
			this.selectorColumnWidth = this.initialSelectorHeaderWidth;
		}
		
		if (this.oldSelectorColumnWidth != this.selectorColumnWidth) {
			this.curveSelectorHeader.setSize(this.selectorColumnWidth-1, this.curveSelectorHeader.getSize().y);
			this.tableSelectorColumn.setWidth(this.selectorColumnWidth-1);
			this.oldSelectorColumnWidth = this.selectorColumnWidth;
			this.application.setGraphicsSashFormWeights(this.selectorColumnWidth, this.windowType);
		}

		log.log(Level.FINER, "curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
	}

	/**
	 * @return the selectorColumnWidth
	 */
	public int getSelectorColumnWidth() {
		return this.selectorColumnWidth;
	}
}
