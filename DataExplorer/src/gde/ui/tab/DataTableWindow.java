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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;

/**
 * Table display class, displays the data in table form
 * @author Winfried Brügmann
 */
public class DataTableWindow extends CTabItem {
	final static Logger					log												= Logger.getLogger(DataTableWindow.class.getName());

	final public static String	TABLE_TIME_STAMP_ABSOLUTE	= "table_time_stamp_absolute";

	Table												dataTable;
	TableColumn									timeColumn;
	TableCursor									cursor;
	Vector<Integer>							rowVector									= new Vector<Integer>(2);
	Vector<Integer>							topindexVector						= new Vector<Integer>(2);

	final DataExplorer					application;
	final Channels							channels;
	final CTabFolder						tabFolder;
	final Menu									popupmenu;
	final TabAreaContextMenu		contextMenu;
	boolean											isAbsoluteDateTime				= false;

	public DataTableWindow(CTabFolder dataTab, int style, int position) {
		super(dataTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = dataTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE+1, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0233));

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
	}

	public void create() {
		this.dataTable = new Table(this.tabFolder, SWT.VIRTUAL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		this.setControl(this.dataTable);
		this.dataTable.setLinesVisible(true);
		this.dataTable.setHeaderVisible(true);

		this.cursor = new TableCursor(this.dataTable, SWT.NONE);
		this.cursor.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent event) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, ("cursor.keyReleased, keycode: " + event.keyCode));

				if (event.stateMask == SWT.MOD1) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Home: go to first row and first column
						DataTableWindow.this.cursor.setSelection(0, 0);
						break;
					case SWT.END:
						// Ctrl+End: go to last row and last column
						DataTableWindow.this.cursor.setSelection(DataTableWindow.this.dataTable.getItemCount() - 1, DataTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					case 0x63:
						// Ctrl+c: copy selection into clip board
						DataTableWindow.this.dataTable.getSelectionCount();
						DataTableWindow.this.dataTable.getSelectionIndices();
						StringBuilder sb = new StringBuilder();
						int columns = DataTableWindow.this.dataTable.getColumnCount();
						for (int i = 0; i < columns; i++) {
							sb.append(DataTableWindow.this.dataTable.getColumn(i).getText().trim()).append('\t');
						}
						sb.deleteCharAt(sb.length() - 1).append('\n');
						for (TableItem tmpItem : DataTableWindow.this.dataTable.getSelection()) {
							for (int i = 0; i < columns; i++) {
								sb.append(tmpItem.getText(i).trim()).append('\t');
							}
							sb.deleteCharAt(sb.length() - 1).append('\n');
						}
						Clipboard cb = new Clipboard(GDE.display);
						String textData = sb.toString();
						Transfer textTransfer = TextTransfer.getInstance();
						cb.setContents(new Object[] { textData }, new Transfer[] { textTransfer });
						break;
					}
				}
				//else if (event.stateMask == SWT.MOD2) {
				//selection of multiple rows (Shift+Arrow Up/Down) require DataTableWindow.this.cursor.setVisible(false);
				//afterwards dataTable.keyReleased handles selection movement
				//}
				else if (((event.stateMask & SWT.MOD2) != 0) && ((event.stateMask & SWT.MOD1) != 0)) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Shift+Home: go to first row
						DataTableWindow.this.cursor.setSelection(0, DataTableWindow.this.cursor.getColumn());
						break;
					case SWT.END:
						// Ctrl+Shift+End: go to last row
						DataTableWindow.this.cursor.setSelection(DataTableWindow.this.dataTable.getItemCount() - 1, DataTableWindow.this.cursor.getColumn());
						break;
					}
				}
				else if (event.stateMask == 0) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Home: go to first column
						workaroundTableCursor(0);
						break;
					case SWT.END:
						// End: go to last column
						workaroundTableCursor(DataTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					}
				}

				// setSelection() doesn't fire a widgetSelected() event, so we need manually update the vector
				if (DataTableWindow.this.cursor.getRow() != null && ((event.stateMask & SWT.MOD1) != 0 && event.character != 'c')) {
					updateVector(DataTableWindow.this.dataTable.indexOf(DataTableWindow.this.cursor.getRow()), DataTableWindow.this.dataTable.getTopIndex());

					//select the table row, after repositioning cursor (HOME/END)
					DataTableWindow.this.dataTable.setSelection(new TableItem[] { DataTableWindow.this.cursor.getRow() });
				}
			}

			/**
			 * Problem: When our code for the TableCursor keyReleased() is reached,
			 * the TableCursor has already internally processed the home/end keyevent
			 * and changed the row/column, see:
			 * http://www.javadocexamples.com/org/eclipse/swt/custom/org.eclipse.swt.custom.TableCursor-source.html
			 * 
			 * Therefore, we need a Vector that stores the last cell positions, so we can
			 * access the position before the current position.
			 * 
			 * @param col
			 */
			private void workaroundTableCursor(int col) {
				/* Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row. 
				*/
				int row = DataTableWindow.this.rowVector.get(0);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, ("Setting selection to row: " + row));
				DataTableWindow.this.cursor.setSelection(row, col);

				/* As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, ("Setting top index: " + DataTableWindow.this.topindexVector.get(0)));
				DataTableWindow.this.dataTable.setTopIndex(DataTableWindow.this.topindexVector.get(0));

				/* Workaround: When Home or End is pressed, the cursor is misplaced in such way,
				 * that the active cell seems to be another one. However, when you press cursor down,
				 * the cell cursor magically appears one cell below the former expected cell.
				 * Calling setVisible fixes this problem.
				 */
				DataTableWindow.this.cursor.setVisible(true);
			}

			public void keyPressed(KeyEvent event) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "cursor.keyPressed " + event); //$NON-NLS-1$
				if (DataTableWindow.this.cursor.getRow() != null && ((event.stateMask & SWT.MOD1) != 0 && event.character != 'c')) {
					//select the table row where the cursor get moved to
					DataTableWindow.this.dataTable.setSelection(new TableItem[] { DataTableWindow.this.cursor.getRow() });
				}
				//to enable multi selection the cursor needs to be set invisible, if the cursor is invisible the dataTable key listener takes effect
				//the cursor should be set to invisible only when shift key is pressed, not while shift is pressed in combination with others
				if (event.keyCode == SWT.MOD2 && ((event.stateMask & SWT.MOD2) == 0) && ((event.stateMask & SWT.MOD1) == 0) && ((event.stateMask & SWT.MOD3) == 0)) {
					DataTableWindow.this.cursor.setVisible(false);
					if (DataTableWindow.this.cursor.getRow() != null) {
						//reset previous (multiple) selection table row(s)
						DataTableWindow.this.dataTable.setSelection(new TableItem[] { DataTableWindow.this.cursor.getRow() });
					}
				}
			}
		});

		// Add the event handling
		this.cursor.addSelectionListener(new SelectionAdapter() {
			// This is called as the user navigates around the table
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "cursor.widgetSelected " + event); //$NON-NLS-1$
				if (DataTableWindow.this.cursor.getRow() != null) {
					updateVector(DataTableWindow.this.dataTable.indexOf(DataTableWindow.this.cursor.getRow()), DataTableWindow.this.dataTable.getTopIndex());
				}
			}
		});

		this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.dataTable.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "dataTable.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_6.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.dataTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				Channel activeChannel = DataTableWindow.this.channels.getActiveChannel();
				if (activeChannel != null) {
					RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
					if (activeRecordSet != null && activeRecordSet.getRecordDataSize(true) > 0) {
						TableItem item = (TableItem) event.item;
						int index = DataTableWindow.this.dataTable.indexOf(item);
						item.setText(activeRecordSet.getDataTableRow(index, DataTableWindow.this.isAbsoluteDateTime));
					}
				}
			}
		});
		this.dataTable.addKeyListener(new KeyListener() {
			int	selectionFlowIndex	= 0;

			private void workaroundTableCursor(int col) {
				/* Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row. 
				*/
				int row = DataTableWindow.this.rowVector.get(DataTableWindow.this.rowVector.size() - 1);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, ("Setting selection to row: " + row));
				DataTableWindow.this.cursor.setSelection(row, col);

				/* As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, ("Setting top index: " + DataTableWindow.this.topindexVector.get(0)));
				DataTableWindow.this.dataTable.setTopIndex(DataTableWindow.this.topindexVector.get(DataTableWindow.this.topindexVector.size() - 1));

				DataTableWindow.this.cursor.setVisible(true);

				//calling setFocus(releases table key listener and take over to cursor key listener again
				DataTableWindow.this.cursor.setFocus();
			}

			public void keyReleased(KeyEvent event) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, ("dataTable.keyReleased, keycode: " + event.keyCode));
				if (event.keyCode == SWT.MOD2) {
					int rowIndex = DataTableWindow.this.rowVector.get(DataTableWindow.this.rowVector.size() - 1) + this.selectionFlowIndex;
					//check table bounds reached
					rowIndex = rowIndex < 0 ? 0 : rowIndex > DataTableWindow.this.dataTable.getItems().length - 1 ? DataTableWindow.this.dataTable.getItems().length - 1 : rowIndex;

					updateVector(rowIndex, DataTableWindow.this.dataTable.getTopIndex());
					workaroundTableCursor(DataTableWindow.this.cursor.getColumn());
					this.selectionFlowIndex = 0;
				}
			}

			public void keyPressed(KeyEvent event) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, ("dataTable.keyPressed, keycode: " + event.keyCode));
				if (event.stateMask == SWT.MOD2 && ((event.stateMask & SWT.MOD1) == 0) && ((event.stateMask & SWT.MOD3) == 0)) {
					switch (event.keyCode) {
					case SWT.ARROW_UP:
						--this.selectionFlowIndex;
						break;
					case SWT.ARROW_DOWN:
						++this.selectionFlowIndex;
						break;
					}
				}
			}
		});
		this.contextMenu.createMenu(this.popupmenu, TabAreaContextMenu.TYPE_TABLE);
		this.cursor.setMenu(this.popupmenu);
	}

	/**
	 * Helper function for workaroundTableCursor()
	 * @param row
	 * @param topindex
	 */
	private void updateVector(int row, int topindex) {

		if (this.rowVector.size() == 2) {
			// we only need the last and the current value, so remove the old entries
			this.rowVector.remove(0);
			this.topindexVector.remove(0);
		}

		// now add the new row
		this.rowVector.addElement(row);
		this.topindexVector.addElement(topindex);
	}

	/**
	 * method to set up new header according device properties
	 */
	public void setHeader() {
		// clean old header
		this.dataTable.removeAll();
		TableColumn[] columns = this.dataTable.getColumns();
		for (TableColumn tableColumn : columns) {
			tableColumn.dispose();
		}

		int extentFactor = 9;
		String time = this.isAbsoluteDateTime ? Messages.getString(MessageIds.GDE_MSGT0436) : Messages.getString(MessageIds.GDE_MSGT0234);
		this.timeColumn = new TableColumn(this.dataTable, SWT.CENTER);
		this.timeColumn.setWidth(time.length() * 7);
		this.timeColumn.setText(time);

		// set the new header line
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				for (int i = 0; i < activeRecordSet.size(); i++) {
					Record record = activeRecordSet.get(i);
					StringBuilder sb = new StringBuilder();
					sb.append(record.getName()).append(GDE.STRING_BLANK_LEFT_BRACKET).append(record.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(sb.length() * extentFactor);
					column.setText(sb.toString());
				}
			}
			else {
				IDevice device = this.application.getActiveDevice();
				String[] measurements = device.getMeasurementNames(activeChannel.getNumber());
				for (int i = 0; i < measurements.length; i++) {
					MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
					StringBuilder sb = new StringBuilder();
					sb.append(measurement.getName()).append(GDE.STRING_BLANK).append(GDE.STRING_LEFT_BRACKET).append(measurement.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(sb.length() * extentFactor);
					column.setText(sb.toString());
				}
				if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) { // add aditional header field for padding //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(100);
				}
			}
		}
	}

	/**
	 * @param count of record entries
	 */
	public void setRowCount(int count) {
		this.dataTable.setItemCount(count);
	}

	/**
	 * clean the table, not the header
	 */
	public synchronized void cleanTable() {
		if (this.dataTable != null && !this.dataTable.isDisposed()) this.dataTable.removeAll();
	}

	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.dataTable.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.dataTable.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}

	/**
	 * reset the top index of the data table if required while updating or adding rows
	 */
	public void updateTopIndex() {
		int height = this.dataTable.getClientArea().height;
		int visibleItemCount = height / this.dataTable.getItemHeight() - 1;
		int topIndex = this.dataTable.getTopIndex();
		int itemCount = this.dataTable.getItemCount();
		if (itemCount > visibleItemCount) {
			int newTopIndex = itemCount - visibleItemCount;
			if (topIndex != newTopIndex) this.dataTable.setTopIndex(newTopIndex);
		}
	}

	/**
	 * @param isAbsoluteDateTime the isAbsoluteDateTime to set
	 */
	public void setAbsoluteDateTime(boolean isAbsoluteDateTime) {
		this.isAbsoluteDateTime = isAbsoluteDateTime;
	}

}
