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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import osde.OSDE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Table display class, displays the data in table form
 * @author Winfried Brügmann
 */
public class DataTableWindow {
	final static Logger					  log	= Logger.getLogger(DataTableWindow.class.getName());

	TabItem												table;
	Table													dataTable;
	TableColumn										timeColumn;

	final OpenSerialDataExplorer	application;
	final Channels								channels;
	final TabFolder								tabFolder;

	public DataTableWindow(OpenSerialDataExplorer currenApplication, TabFolder dataTab) {
		this.application = currenApplication;
		this.tabFolder = dataTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.table = new TabItem(this.tabFolder, SWT.NONE);
		this.table.setText(Messages.getString(MessageIds.OSDE_MSGT0233));
		{
			this.dataTable = new Table(this.tabFolder, SWT.BORDER);
			this.table.setControl(this.dataTable);
			this.dataTable.setLinesVisible(true);
			this.dataTable.setHeaderVisible(true);
		}
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
		String time = Messages.getString(MessageIds.OSDE_MSGT0234); //$NON-NLS-1$
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
					sb.append(record.getName()).append(OSDE.STRING_BLANK).append(OSDE.STRING_LEFT_BRACKET).append(record.getUnit()).append(OSDE.STRING_RIGHT_BRACKET);
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
				String channelConfigKey = activeChannel.getConfigKey();
				String[] measurements = device.getMeasurementNames(channelConfigKey);
				for (int i = 0; i < measurements.length; i++) {
					MeasurementType measurement = device.getMeasurement(channelConfigKey, measurements[i]);
					StringBuilder sb = new StringBuilder();
					sb.append(measurement.getName()).append(OSDE.STRING_BLANK).append(OSDE.STRING_LEFT_BRACKET).append(measurement.getUnit()).append(OSDE.STRING_RIGHT_BRACKET);
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
	 * method to update the data table pain with actual record data 
	 */
	public void updateDataTable(final RecordSet recordSet) {

		if (recordSet.isTableDisplayable() && recordSet.isTableDataCalculated()) {
			if (log.isLoggable(Level.FINE)) log.fine("entry data table update"); //$NON-NLS-1$
			this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0235));
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

			// cleanup old data table
			this.dataTable.removeAll();

			// display data table
			try {
				int recordEntries = recordSet.getNumberDataTableRows();
				int progressStart = this.application.getProgressPercentage();
				double progressInterval = (100.0 - progressStart) / recordEntries;
				TableItem item;
				
				for (int i = 0; i < recordEntries; i++) {
					this.application.setProgress(new Double(i * progressInterval + progressStart).intValue());
					item = new TableItem(this.dataTable, SWT.RIGHT);
					item.setText(recordSet.getDataPoints(i));
				}
				//dataTable.setVisible(true);
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}

			this.application.setProgress(100);
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
			this.application.setStatusMessage(OSDE.STRING_BLANK);
			if (log.isLoggable(Level.FINE)) log.fine("exit data table update"); //$NON-NLS-1$
		}
	}
	
	public void cleanTable(boolean isDisabled) {
		this.dataTable.removeAll();
		if (isDisabled) {
			TableItem item = new TableItem(this.dataTable, SWT.RIGHT);
			item.setText(Messages.getString(MessageIds.OSDE_MSGT0230).split(OSDE.STRING_BLANK));
		}
	}
}
