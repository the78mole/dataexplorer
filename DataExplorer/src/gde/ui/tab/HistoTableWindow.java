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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.ui.tab;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.HistoRecordSet;
import gde.data.HistoSet;
import gde.data.Record;
import gde.data.RecordSet;
import gde.data.TrailRecord;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;
import java.util.NavigableMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TableEditor;
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

/**
 * Histo display class, displays the histo in table form.
 * @author Thomas Eickert
 */
public class HistoTableWindow extends CTabItem {
	private final static String	$CLASS_NAME						= HistoTableWindow.class.getName();
	private final static Logger	log										= Logger.getLogger($CLASS_NAME);

	private final HistoSet			histoSet							= HistoSet.getInstance();

	Table												dataTable;
	TableColumn									recordsColumn;
	TableColumn									tableCurveTypeColumn;
	TableCursor									cursor;
	Vector<Integer>							rowVector							= new Vector<>(2);
	Vector<Integer>							topindexVector				= new Vector<>(2);

	final DataExplorer					application;
	final Channels							channels;
	final Settings							settings;
	final CTabFolder						tabFolder;
	final Menu									popupmenu;
	final TabAreaContextMenu		contextMenu;

	final int										textExtentFactor			= 7;
	final String								isoDateTimeDelimiter	= "T";															//$NON-NLS-1$

	public HistoTableWindow(CTabFolder dataTab, int style, int position) {
		super(dataTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = dataTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0793));

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
	}

	public void create() {
		this.dataTable = new Table(this.tabFolder, SWT.VIRTUAL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		this.setControl(this.dataTable);
		this.dataTable.setLinesVisible(true);
		this.dataTable.setHeaderVisible(true);

		final TableEditor editor = new TableEditor(this.dataTable);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		this.cursor = new TableCursor(this.dataTable, SWT.NONE);
		this.cursor.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(final KeyEvent event) {
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) HistoTableWindow.log.log(java.util.logging.Level.FINEST, ("cursor.keyReleased, keycode: " + event.keyCode)); //$NON-NLS-1$

				if (event.stateMask == SWT.MOD1) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Home: go to first row and first column
						HistoTableWindow.this.cursor.setSelection(0, 0);
						break;
					case SWT.END:
						// Ctrl+End: go to last row and last column
						HistoTableWindow.this.cursor.setSelection(HistoTableWindow.this.dataTable.getItemCount() - 1, HistoTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					case 0x63:
						// Ctrl+c: copy selection into clip board
						HistoTableWindow.this.dataTable.getSelectionCount();
						HistoTableWindow.this.dataTable.getSelectionIndices();
						StringBuilder sb = new StringBuilder();
						int columns = HistoTableWindow.this.dataTable.getColumnCount();
						for (int i = 0; i < columns; i++) {
							sb.append(HistoTableWindow.this.dataTable.getColumn(i).getText().trim()).append('\t');
						}
						sb.deleteCharAt(sb.length() - 1).append('\n');
						for (TableItem tmpItem : HistoTableWindow.this.dataTable.getSelection()) {
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
				// else if (event.stateMask == SWT.MOD2) {
				// selection of multiple rows (Shift+Arrow Up/Down) require DataTableWindow.this.cursor.setVisible(false);
				// afterwards dataTable.keyReleased handles selection movement
				// }
				else if (((event.stateMask & SWT.MOD2) != 0) && ((event.stateMask & SWT.MOD1) != 0)) {
					switch (event.keyCode) {
					case SWT.HOME:
						// Ctrl+Shift+Home: go to first row
						HistoTableWindow.this.cursor.setSelection(0, HistoTableWindow.this.cursor.getColumn());
						break;
					case SWT.END:
						// Ctrl+Shift+End: go to last row
						HistoTableWindow.this.cursor.setSelection(HistoTableWindow.this.dataTable.getItemCount() - 1, HistoTableWindow.this.cursor.getColumn());
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
						workaroundTableCursor(HistoTableWindow.this.dataTable.getColumnCount() - 1);
						break;
					}
				}

				// setSelection() doesn't fire a widgetSelected() event, so we need manually update the vector
				// System.out.println("cursor.keyReleased " + (event.stateMask & SWT.MOD1) + " - " + event.keyCode );
				if (HistoTableWindow.this.cursor.getRow() != null && (((event.stateMask & SWT.MOD1) == 0 && event.character != 'c') || ((event.stateMask & SWT.MOD1) == 0 && event.character != 'a'))) {
					updateVector(HistoTableWindow.this.dataTable.indexOf(HistoTableWindow.this.cursor.getRow()), HistoTableWindow.this.dataTable.getTopIndex());

					// select the table row, after repositioning cursor (HOME/END)
					HistoTableWindow.this.dataTable.setSelection(new TableItem[] { HistoTableWindow.this.cursor.getRow() });
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
				/*
				 * Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row.
				 */
				int row = HistoTableWindow.this.rowVector.get(0);
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINER)) HistoTableWindow.log.log(java.util.logging.Level.FINER, ("Setting selection to row: " + row)); //$NON-NLS-1$
				HistoTableWindow.this.cursor.setSelection(row, col);

				/*
				 * As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINER))
					HistoTableWindow.log.log(java.util.logging.Level.FINER, ("Setting top index: " + HistoTableWindow.this.topindexVector.get(0))); //$NON-NLS-1$
				HistoTableWindow.this.dataTable.setTopIndex(HistoTableWindow.this.topindexVector.get(0));

				/*
				 * Workaround: When Home or End is pressed, the cursor is misplaced in such way,
				 * that the active cell seems to be another one. However, when you press cursor down,
				 * the cell cursor magically appears one cell below the former expected cell.
				 * Calling setVisible fixes this problem.
				 */
				HistoTableWindow.this.cursor.setVisible(true);
			}

			@Override
			public void keyPressed(KeyEvent event) {
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) HistoTableWindow.log.log(java.util.logging.Level.FINEST, "cursor.keyPressed " + event); //$NON-NLS-1$
				// System.out.println("cursor.keyPressed " + (event.stateMask & SWT.MOD1) + " - " + event.keyCode );
				if (HistoTableWindow.this.cursor.getRow() != null && !(event.stateMask == SWT.MOD1 && event.keyCode != 0x99) && !(event.stateMask == SWT.MOD1 && event.keyCode != 0x97)
						&& event.keyCode != SWT.MOD1) {
					// select the table row where the cursor get moved to
					HistoTableWindow.this.dataTable.setSelection(new TableItem[] { HistoTableWindow.this.cursor.getRow() });
				}
				else if (HistoTableWindow.this.cursor.getRow() != null && (event.stateMask & SWT.MOD1) == SWT.MOD1 && event.keyCode == 0x61) {
					// System.out.println("select all");
					HistoTableWindow.this.dataTable.setSelection(HistoTableWindow.this.dataTable.getItems());
				}
				// to enable multi selection the cursor needs to be set invisible, if the cursor is invisible the dataTable key listener takes effect
				// the cursor should be set to invisible only when shift key is pressed, not while shift is pressed in combination with others
				if (event.keyCode == SWT.MOD2 && ((event.stateMask & SWT.MOD2) == 0) && ((event.stateMask & SWT.MOD1) == 0) && ((event.stateMask & SWT.MOD3) == 0)) {
					HistoTableWindow.this.cursor.setVisible(false);
					if (HistoTableWindow.this.cursor.getRow() != null) {
						// reset previous (multiple) selection table row(s)
						HistoTableWindow.this.dataTable.setSelection(new TableItem[] { HistoTableWindow.this.cursor.getRow() });
					}
				}
			}
		});

		// Add the event handling
		this.cursor.addSelectionListener(new SelectionAdapter() {
			// This is called as the user navigates around the table
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) HistoTableWindow.log.log(java.util.logging.Level.FINEST, "cursor.widgetSelected " + event); //$NON-NLS-1$
				if (HistoTableWindow.this.cursor.getRow() != null) {
					updateVector(HistoTableWindow.this.dataTable.indexOf(HistoTableWindow.this.cursor.getRow()), HistoTableWindow.this.dataTable.getTopIndex());
				}
			}
		});

		this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.dataTable.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINER)) HistoTableWindow.log.log(java.util.logging.Level.FINER, "dataTable.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_6.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.dataTable.addListener(SWT.SetData, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (HistoTableWindow.this.histoSet.getTrailRecordSet().size() > 0) {
					TableItem item = (TableItem) event.item;
					TrailRecord trailRecord = (TrailRecord) HistoTableWindow.this.histoSet.getTrailRecordSet().getVisibleAndDisplayableRecordsForTable().get(HistoTableWindow.this.dataTable.indexOf(item));
					item.setText(trailRecord.getHistoTableRow());
				}
			}
		});

		this.dataTable.addKeyListener(new KeyListener() {
			int selectionFlowIndex = 0;

			private void workaroundTableCursor(int col) {
				/*
				 * Get the second element from the vector.
				 * This represents the row that was active before the keyevent
				 * fired and the TableCursor automatically changed the row.
				 */
				int row = HistoTableWindow.this.rowVector.get(HistoTableWindow.this.rowVector.size() - 1);
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINER)) HistoTableWindow.log.log(java.util.logging.Level.FINER, ("Setting selection to row: " + row)); //$NON-NLS-1$
				HistoTableWindow.this.cursor.setSelection(row, col);

				/*
				 * As the TableCursor automatically changes the rows and we go back, the item that was on top of the list changes.
				 * We fix that here to get the original item at the top of the list again.
				 */
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINER))
					HistoTableWindow.log.log(java.util.logging.Level.FINER, ("Setting top index: " + HistoTableWindow.this.topindexVector.get(0))); //$NON-NLS-1$
				HistoTableWindow.this.dataTable.setTopIndex(HistoTableWindow.this.topindexVector.get(HistoTableWindow.this.topindexVector.size() - 1));

				HistoTableWindow.this.cursor.setVisible(true);

				// calling setFocus(releases table key listener and take over to cursor key listener again
				HistoTableWindow.this.cursor.setFocus();
			}

			@Override
			public void keyReleased(KeyEvent event) {
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) HistoTableWindow.log.log(java.util.logging.Level.FINEST, ("dataTable.keyReleased, keycode: " + event.keyCode)); //$NON-NLS-1$
				if (event.keyCode == SWT.MOD2) {
					int rowIndex = HistoTableWindow.this.rowVector.get(HistoTableWindow.this.rowVector.size() - 1) + this.selectionFlowIndex;
					// check table bounds reached
					rowIndex = rowIndex < 0 ? 0 : rowIndex > HistoTableWindow.this.dataTable.getItems().length - 1 ? HistoTableWindow.this.dataTable.getItems().length - 1 : rowIndex;

					updateVector(rowIndex, HistoTableWindow.this.dataTable.getTopIndex());
					workaroundTableCursor(HistoTableWindow.this.cursor.getColumn());
					this.selectionFlowIndex = 0;
					switch (event.keyCode) {
					case 1:
						break;
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent event) {
				if (HistoTableWindow.log.isLoggable(java.util.logging.Level.FINEST)) HistoTableWindow.log.log(java.util.logging.Level.FINEST, ("dataTable.keyPressed, keycode: " + event.keyCode)); //$NON-NLS-1$
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
	 * query if this component is visible 
	 * @return true if table window is visible
	 */
	public boolean isVisible() {
		return this.dataTable.isVisible();
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
	 * set up two header columns and columns for the trail recordsets of the histoSet timestamp range.
	 */
	public void setHeader() {
		// clean old header
		this.dataTable.removeAll(); // prevent to display flickering (to some extent only)
		for (TableColumn tableColumn : this.dataTable.getColumns()) {
			tableColumn.dispose();
		}
		setRowCount(0); // ET required for Listener firing on setRowCount(xyz)

		String recordTitle = Messages.getString(MessageIds.GDE_MSGT0352);
		this.recordsColumn = new TableColumn(this.dataTable, SWT.CENTER);
		this.recordsColumn.setWidth(recordTitle.length() * this.textExtentFactor * 30 / 10);
		this.recordsColumn.setText(recordTitle);

		String curveTypeHeader = Messages.getString(MessageIds.GDE_MSGT0828);
		this.recordsColumn = new TableColumn(this.dataTable, SWT.LEFT);
		this.recordsColumn.setWidth(curveTypeHeader.length() * this.textExtentFactor * 15 / 10);
		this.recordsColumn.setText(curveTypeHeader);

		if (this.histoSet.size() > 0) {
			// get the timestamp range and sort order
			long minimumTimeStamp = this.histoSet.lastKey();
			long maximumTimeStamp = this.histoSet.firstKey();
			NavigableMap<Long, List<HistoRecordSet>> histoSubSet = this.histoSet.subMap(maximumTimeStamp, true, minimumTimeStamp, true);
			if (!settings.isXAxisReversed()) {
				histoSubSet = histoSubSet.descendingMap();
			}

			// set the data columns of the new header line
			Channel activeChannel = this.channels.getActiveChannel();
			if (activeChannel != null) {
				if (histoSubSet.size() > 0) {
					for (List<HistoRecordSet> cachedRecordSets : histoSubSet.values()) {
						for (RecordSet recordSet : cachedRecordSets) {
							StringBuilder sb = new StringBuilder();
							ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(recordSet.getStartTimeStamp()), ZoneId.systemDefault());
							sb.append(zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(this.isoDateTimeDelimiter, GDE.STRING_BLANK));
							TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
							column.setWidth(sb.length() * this.textExtentFactor * 8 / 10);
							column.setText(sb.toString());
						}
					}
				}
				else {
					if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) { //todo required??? add aditional header field for padding //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
						column.setWidth(100);
					}
				}
			}

		}
		log.log(java.util.logging.Level.FINER, "dataTable.getColumnCount() " + this.dataTable.getColumnCount()); //$NON-NLS-1$
	}

	/**
	 * triggers the table listener SWT.SetData based on the visible and displayable records.
	 * @param count of visible (!) record entries
	 */
	public void setRowCount(int count) {
		this.dataTable.setItemCount(count);
	}

	/**
	 * clean the table, not the header
	 */
	public synchronized void cleanTable() {
		if (this.dataTable != null && !this.dataTable.isDisposed()) {
			this.dataTable.removeAll();
			// ET added (empty columns remain after removeAll)
			for (TableColumn tableColumn : this.dataTable.getColumns()) {
				tableColumn.dispose();
			}
		}
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

	public int getTableLength() {
		return this.dataTable.getItems().length;
	}

}
