/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
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

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Table display class, displays the data in table form
 * @author Winfried Brügmann
 */
public class DataTableWindow {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private TabItem												table;
	private Table													dataTable;
	private TableColumn										timeColumn;

	private final OpenSerialDataExplorer	application;
	private final Channels								channels;
	private final TabFolder								tabFolder;

	public DataTableWindow(OpenSerialDataExplorer application, TabFolder dataTab) {
		this.application = application;
		this.tabFolder = dataTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		table = new TabItem(tabFolder, SWT.NONE);
		table.setText("Tabelle");
		{
			dataTable = new Table(tabFolder, SWT.BORDER);
			table.setControl(dataTable);
			dataTable.setLinesVisible(true);
			dataTable.setHeaderVisible(true);
		}
	}

	/**
	 * method to set up new header according device properties
	 */
	public void setHeader() {
		// clean old header
		dataTable.removeAll();
		TableColumn[] columns = dataTable.getColumns();
		for (TableColumn tableColumn : columns) {
			tableColumn.dispose();
		}

		int extentFactor = 9;
		String time = "Zeit [sec]";
		timeColumn = new TableColumn(dataTable, SWT.CENTER);
		timeColumn.setWidth(time.length() * extentFactor);
		timeColumn.setText(time);

		// set the new header line
		IDevice device = application.getActiveDevice();
		Channel activeChannel = channels.getActiveChannel();
		if (activeChannel != null) {
			String channelConfigKey = activeChannel.getConfigKey();
			String[] measurements = device.getMeasurementNames(channelConfigKey);
			for (int i = 0; i < measurements.length; i++) {
				MeasurementType measurement = device.getMeasurement(channelConfigKey, measurements[i]);
				StringBuilder sb = new StringBuilder();
				sb.append(measurement.getName()).append(" [").append(measurement.getUnit()).append("]");
				TableColumn column = new TableColumn(dataTable, SWT.CENTER);
				column.setWidth(sb.length() * extentFactor);
				column.setText(sb.toString());
			}
			if (System.getProperty("os.name", "").toLowerCase().startsWith("linux")) { // add aditional header field for padding
				TableColumn column = new TableColumn(dataTable, SWT.CENTER);
				column.setWidth(100);
			}
		}
	}

	/**
	 * method to update the data table pain with actual record data 
	 */
	public void updateDataTable(final Channel channel, final RecordSet recordSet) {

		if (recordSet.isTableDisplayable() && recordSet.isTableDataCalculated()) {
			if (log.isLoggable(Level.FINE)) log.fine("entry data table update");
			application.setStatusMessage(" -> erneuere Datentabelle");				
			application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

			// cleanup old data table
			dataTable.removeAll();

			// display data table
			try {
				int recordEntries = recordSet.getNumberDataTableRows();
				int progressStart = application.getProgressPercentage();
				double progressInterval = (100.0 - progressStart) / recordEntries;
				TableItem item;
				
				for (int i = 0; i < recordEntries; i++) {
					application.setProgress(new Double(i * progressInterval + progressStart).intValue());
					item = new TableItem(dataTable, SWT.RIGHT);
					item.setText(recordSet.getDataPoints(i));
				}
				dataTable.setVisible(true);
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}

			application.setProgress(100);
			application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
			application.setStatusMessage(" ");
			if (log.isLoggable(Level.FINE)) log.fine("exit data table update");
		}
	}
	
	public void cleanTable(boolean isDisabled) {
		dataTable.removeAll();
		if (isDisabled) {
			TableItem item = new TableItem(dataTable, SWT.RIGHT);
			item.setText(new String[] {"Die", "Anzeige",  "ist",  "ausgeschaltet!"});
		}
	}
}
