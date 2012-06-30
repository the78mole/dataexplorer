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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
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
	final static Logger					  log	= Logger.getLogger(DataTableWindow.class.getName());

	final public static String		TABLE_TIME_STAMP_ABSOLUTE = "table_time_stamp_absolute";

	Table													dataTable;
	TableColumn										timeColumn;

	final DataExplorer						application;
	final Channels								channels;
	final CTabFolder							tabFolder;
	final Menu										popupmenu;
	final TabAreaContextMenu			contextMenu;
	boolean												isAbsoluteDateTime = false;

	public DataTableWindow(CTabFolder dataTab, int style, int position) {
		super(dataTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = dataTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0233));
		
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
	}

	public void create() {
		this.dataTable = new Table(this.tabFolder, SWT.VIRTUAL | SWT.BORDER);
		this.setControl(this.dataTable);
		this.dataTable.setLinesVisible(true);
		this.dataTable.setHeaderVisible(true);
		this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.dataTable.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "DigitalDisplay.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_6.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.dataTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				Channel activeChannel = channels.getActiveChannel();
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
		this.contextMenu.createMenu(this.popupmenu, TabAreaContextMenu.TYPE_TABLE);
		this.dataTable.setMenu(this.popupmenu);
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
		String time = isAbsoluteDateTime ? Messages.getString(MessageIds.GDE_MSGT0436) : Messages.getString(MessageIds.GDE_MSGT0234); 
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
				if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) { // add aditional header field for padding //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
					column.setWidth(100);
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
		this.dataTable.setItemCount (count);
	}
	
	/**
	 * clean the table, not the header
	 */
	public synchronized void cleanTable() {
		if (this.dataTable != null && !this.dataTable.isDisposed())
			this.dataTable.removeAll();
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
			if (topIndex != newTopIndex)
				this.dataTable.setTopIndex(newTopIndex);
		}
	}

	/**
	 * @param isAbsoluteDateTime the isAbsoluteDateTime to set
	 */
	public void setAbsoluteDateTime(boolean isAbsoluteDateTime) {
		this.isAbsoluteDateTime = isAbsoluteDateTime;
	}
}
