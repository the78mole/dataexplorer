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
****************************************************************************************/
package osde.ui.tab;

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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import osde.DE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.log.Level;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Table display class, displays the data in table form
 * @author Winfried Brügmann
 */
public class DataTableWindow extends CTabItem {
	final static Logger					  log	= Logger.getLogger(DataTableWindow.class.getName());

	Table													dataTable;
	TableColumn										timeColumn;

	final DataExplorer	application;
	final Channels								channels;
	final CTabFolder							tabFolder;

	public DataTableWindow(CTabFolder dataTab, int style, int position) {
		super(dataTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = dataTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.DE_MSGT0233));
	}

	public void create() {
		this.dataTable = new Table(this.tabFolder, SWT.VIRTUAL | SWT.BORDER);
		this.setControl(this.dataTable);
		this.dataTable.setLinesVisible(true);
		this.dataTable.setHeaderVisible(true);
		this.dataTable.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.dataTable.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				log.log(Level.FINER, "DigitalDisplay.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_6.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.dataTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				Channel activeChannel = channels.getActiveChannel();
				if (activeChannel != null) {
					RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
					if (activeRecordSet != null) {
						TableItem item = (TableItem) event.item;
						int index = DataTableWindow.this.dataTable.indexOf(item);
						item.setText("Item " + index);
						item.setText(activeRecordSet.getDataTableRow(index));
						//System.out.println(item.getText());
					}
				}
			}
		});
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
		String time = Messages.getString(MessageIds.DE_MSGT0234); //$NON-NLS-1$
		this.timeColumn = new TableColumn(this.dataTable, SWT.CENTER);
		this.timeColumn.setWidth(time.length() * extentFactor);
		this.timeColumn.setText(time);

		// set the new header line
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				String[] recordNames = activeRecordSet.getRecordNames();
				for (int i = 0; i < recordNames.length; i++) {
					Record record = activeRecordSet.get(recordNames[i]);
					StringBuilder sb = new StringBuilder();
					sb.append(record.getName()).append(DE.STRING_BLANK).append(DE.STRING_LEFT_BRACKET).append(record.getUnit()).append(DE.STRING_RIGHT_BRACKET);
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
					sb.append(measurement.getName()).append(DE.STRING_BLANK).append(DE.STRING_LEFT_BRACKET).append(measurement.getUnit()).append(DE.STRING_RIGHT_BRACKET);
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
	 * @param recordEntries
	 */
	public void setRowCount(int count) {
		this.dataTable.setItemCount (count);
	}
	
	public void cleanTable() {
		if (this.dataTable != null && !this.dataTable.isDisposed())
			this.dataTable.removeAll();
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.dataTable.getClientArea();
		Image tabContentImage = new Image(DataExplorer.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.dataTable.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}
}
