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
//		this.addPaintListener(new PaintListener() {
//			public void paintControl(PaintEvent evt) {
//				log.info("SelectorComposite.paintControl() = " + evt);
//				doUpdateCurveSelectorTable();
//			}
//		});
//		this.addControlListener(new ControlListener() {
//			public void controlResized(ControlEvent evt) {
//				log.info("SelectorComposite.controlResized() = " + evt);
//				Point size = SelectorComposite.this.getSize();
//				if (!SelectorComposite.this.oldSize.equals(size)) {
//					log.info(SelectorComposite.this.oldSize + " - " + size);
//					SelectorComposite.this.oldSize = size;
//					doUpdateCurveSelectorTable();
//				}
//			}
//			public void controlMoved(ControlEvent evt) {
//				log.finest("SelectorComposite.controlMoved() = " + evt);
//			}
//		});
		this.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				log.finer("curveSelector.helpRequested " + evt); //$NON-NLS-1$
				SelectorComposite.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		{
			this.curveSelectorHeader = new CLabel(this, SWT.NONE);
			this.curveSelectorHeader.setText("  " + Messages.getString(MessageIds.OSDE_MSGT0254)); //$NON-NLS-1$
			this.curveSelectorHeader.setFont(SWTResourceManager.getFont(this.curveSelectorHeader, SWT.BOLD));
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
			this.curveSelectorTable.setLinesVisible(true);
			FormData curveTableLData = new FormData();
			curveTableLData.width = 82;
			curveTableLData.height = 457;
			curveTableLData.left = new FormAttachment(0, 1000, 0);
			curveTableLData.top = new FormAttachment(0, 1000, 25);
			curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
			curveTableLData.right = new FormAttachment(1000, 1000, 0);
			this.curveSelectorTable.setLayoutData(curveTableLData);

			//this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
			if (this.popupmenu != null) this.curveSelectorTable.setMenu(this.popupmenu);
			//this.curveSelectorTable.layout();
			//TODO this.contextMenu = new CurveSelectorContextMenu();
			//this.contextMenu.createMenu(this.popupmenu);
//			this.curveSelectorTable.addPaintListener(new PaintListener() {
//				public void paintControl(PaintEvent evt) {
//					log.finest("curveSelectorTable.paintControl, event=" + evt); //$NON-NLS-1$
//					doUpdateCurveSelectorTable();
//				}
//			});
			this.curveSelectorTable.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.finest("curveTable.widgetSelected, event=" + evt); //$NON-NLS-1$
					TableItem item = (TableItem) evt.item;
					String recordName = ((TableItem) evt.item).getText();
					log.fine("selected = " + recordName); //$NON-NLS-1$
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
							if (item.getChecked()) {
								activeRecord.setVisible(true);
								SelectorComposite.this.popupmenu.getItem(0).setSelection(true);
								item.setData(OpenSerialDataExplorer.OLD_STATE, true);
								item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
								SelectorComposite.this.application.updateGraphicsWindow();
								SelectorComposite.this.application.updateDigitalWindow();
								SelectorComposite.this.application.updateAnalogWindow();
								SelectorComposite.this.application.updateCellVoltageWindow();
								SelectorComposite.this.application.updateFileCommentWindow();
							}
							else {
								activeRecord.setVisible(false);
								SelectorComposite.this.popupmenu.getItem(0).setSelection(false);
								item.setData(OpenSerialDataExplorer.OLD_STATE, false);
								item.setData(GraphicsWindow.WINDOW_TYPE, SelectorComposite.this.windowType);
								SelectorComposite.this.application.updateGraphicsWindow();
								SelectorComposite.this.application.updateDigitalWindow();
								SelectorComposite.this.application.updateAnalogWindow();
								SelectorComposite.this.application.updateCellVoltageWindow();
								SelectorComposite.this.application.updateFileCommentWindow();
							}
							activeRecord.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);

						}
						else {
							log.log(Level.WARNING, "GraphicsWindow.type = " + SelectorComposite.this.windowType + " recordName = \"" + recordName + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
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
				this.curveSelectorTable.removeAll();
				//this.curveSelectorHeader.pack(true);
				//itemWidth = this.selectorHeaderWidth = this.curveSelectorHeader.getSize().x;
				String[] recordKeys = recordSet.getRecordNames();
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
					if (log.isLoggable(Level.FINER)) log.finer(record.getName());

					TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
					item.setForeground(record.getColor());
					item.setText(record.getName());
					int textSize = record.getName().length() * 8;
					//this.curveSelectorTable.pack();
					//log.info(item.getText() + " " + item.getBounds().width);
					int checkBoxWidth = 20;
					if (itemWidth < (textSize+checkBoxWidth)) itemWidth = textSize+checkBoxWidth;
					//log.info(item.getText() + " " + itemWidth);
					//item.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.jpg"));
					if (record.isDisplayable()) {
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
					else {
						item.setChecked(false);
						item.setData(OpenSerialDataExplorer.OLD_STATE, false);
						item.setData(GraphicsWindow.WINDOW_TYPE, this.windowType);
						item.dispose();
					}
				}
				this.selectorColumnWidth = itemWidth;
				if (log.isLoggable(Level.FINER)) log.fine("*curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
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

		if (log.isLoggable(Level.FINER)) log.finer("curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
	}

	/**
	 * @return the selectorColumnWidth
	 */
	public int getSelectorColumnWidth() {
		return this.selectorColumnWidth;
	}
}
