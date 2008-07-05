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

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Class to enable a file comment
 * @author Winfried Brügmann
 */
public class FileCommentWindow {

	final static Logger					log	= Logger.getLogger(FileCommentWindow.class.getName());

	TabItem					commentTab;
	Composite				commentMainComposite;
	CLabel 					infoLabel;
	Text						fileCommentText;
	Table						recordCommentTable;
	TableColumn			recordCommentTableHeader;
	TableColumn			recordCommentTableHeader2;

	final Channels	channels;
	final TabFolder	displayTab;

	/**
	 * constructor with TabFolder parent
	 * @param currentDisplayTab
	 */
	public FileCommentWindow(TabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.channels = Channels.getInstance();
	}

	/**
	 * method to create the window and register required event listener
	 */
	public void create() {
		this.commentTab = new TabItem(this.displayTab, SWT.NONE);
		this.commentTab.setText(Messages.getString(MessageIds.OSDE_MSGT0239));
		SWTResourceManager.registerResourceUser(this.commentTab);

		{
			this.commentMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.commentTab.setControl(this.commentMainComposite);
			this.commentMainComposite.setLayout(null);
			this.commentMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("commentMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
					updateRecordSetTable();
				}
			});
			{
				this.infoLabel = new CLabel(this.commentMainComposite, SWT.LEFT);
				this.infoLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0240));
				this.infoLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false)); //$NON-NLS-1$
				this.infoLabel.setBounds(50, 10, 500, 26);
			}
			{
				this.fileCommentText = new Text(this.commentMainComposite, SWT.WRAP | SWT.MULTI | SWT.BORDER  | SWT.V_SCROLL);
				this.fileCommentText.setText(Messages.getString(MessageIds.OSDE_MSGT0241));
				this.fileCommentText.setBounds(50, 40, 500, 100);
				this.fileCommentText.setText(this.channels.getFileDescription());
				this.fileCommentText.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.finer("fileCommentText.helpRequested " + evt); //$NON-NLS-1$
						OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_10.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				this.fileCommentText.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent evt) {
						log.finest("recordSelectCombo.keyPressed, event=" + evt); //$NON-NLS-1$
						if (evt.character == SWT.CR) {
								FileCommentWindow.this.channels.setFileDescription(FileCommentWindow.this.fileCommentText.getText());
						}
					}
				});
			}
			{
			this.recordCommentTable = new Table(this.commentMainComposite, SWT.BORDER | SWT.V_SCROLL);
			this.recordCommentTable.setBounds(50, 200, 500, 100);
			//this.table.setControl(this.dataTable);
			this.recordCommentTable.setLinesVisible(true);
			this.recordCommentTable.setHeaderVisible(true);

			this.recordCommentTableHeader = new TableColumn(this.recordCommentTable, SWT.LEFT);
			this.recordCommentTableHeader.setWidth(250);
			this.recordCommentTableHeader.setText(Messages.getString(MessageIds.OSDE_MSGT0242));

			this.recordCommentTableHeader2 = new TableColumn(this.recordCommentTable, SWT.LEFT);
			this.recordCommentTableHeader2.setWidth(500);
			this.recordCommentTableHeader2.setText(Messages.getString(MessageIds.OSDE_MSGT0243));
}
		}
	}
	
	public void update() {
		if (this.channels.getActiveChannel() != null) {
			this.fileCommentText.setText(this.channels.getFileDescription());
		}
		updateRecordSetTable();
	}

	/**
	 * update the record set enty table
	 */
	void updateRecordSetTable() {
		Point mainSize = FileCommentWindow.this.commentMainComposite.getSize();
		//log.info("mainSize = " + mainSize.toString());
		Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
				, mainSize.x * 90/100, mainSize.y * 40/100);
		//log.info("cover bounds = " + bounds.toString());
		FileCommentWindow.this.infoLabel.setBounds(50, 10, bounds.width, bounds.y-10);
		FileCommentWindow.this.fileCommentText.setBounds(bounds);
		FileCommentWindow.this.fileCommentText.setText(FileCommentWindow.this.channels.getFileDescription());
		
		bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 50/100
				, mainSize.x * 90/100, mainSize.y * 40/100);
		FileCommentWindow.this.recordCommentTable.setBounds(bounds);
		FileCommentWindow.this.recordCommentTable.removeAll();
		FileCommentWindow.this.recordCommentTableHeader2.setWidth(bounds.width-205);
		Channel channel = Channels.getInstance().getActiveChannel();
		TableItem item;
		if (channel != null) {
				HashMap<String, RecordSet> recordSets = channel.getRecordSets();
				for (String recordSetKey : channel.getRecordSetNames()) {
					if (recordSetKey != null) {
						item = new TableItem(FileCommentWindow.this.recordCommentTable, SWT.LEFT);
						item.setText(new String[] { recordSetKey, recordSets.get(recordSetKey).getRecordSetDescription() });
					}
				}
		}
	}
}