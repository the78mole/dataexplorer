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
import org.eclipse.swt.widgets.Text;

import osde.data.Channels;
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
		this.commentTab.setText("Dateikommentar");
		SWTResourceManager.registerResourceUser(this.commentTab);

		{
			this.commentMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.commentTab.setControl(this.commentMainComposite);
			this.commentMainComposite.setLayout(null);
			this.commentMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("cellVoltageMainComposite.paintControl, event=" + evt);
					Point mainSize = FileCommentWindow.this.commentMainComposite.getSize();
					//log.info("mainSize = " + mainSize.toString());
					Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
							, mainSize.x * 90/100, mainSize.y * 80/100);
					//log.info("cover bounds = " + bounds.toString());
					FileCommentWindow.this.infoLabel.setBounds(50, 10, bounds.width, bounds.y-10);
					FileCommentWindow.this.fileCommentText.setBounds(bounds);
				}
			});
			{
				this.infoLabel = new CLabel(this.commentMainComposite, SWT.LEFT);
				this.infoLabel.setText("Dateikommentar");
				this.infoLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
				this.infoLabel.setBounds(50, 10, 500, 26);
			}
			this.commentMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.finer("commentMainComposite.paintControl, event=" + evt);
					FileCommentWindow.this.fileCommentText.setText(FileCommentWindow.this.channels.getFileDescription());
				}
			});
			{
				this.fileCommentText = new Text(this.commentMainComposite, SWT.LEFT | SWT.TOP | SWT.MULTI | SWT.WRAP | SWT.BORDER);
				this.fileCommentText.setText("Dateikommentar : ");
				this.fileCommentText.setBounds(50, 40, 500, 300);
				this.fileCommentText.setText(this.channels.getFileDescription());
				this.fileCommentText.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.finer("fileCommentText.helpRequested " + evt);
						OpenSerialDataExplorer.getInstance().openHelpDialog("OpenSerialDataExplorer", "HelpInfo_10.html");
					}
				});
				this.fileCommentText.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent evt) {
						log.finest("recordSelectCombo.keyPressed, event=" + evt);
						if (evt.character == SWT.CR) {
								FileCommentWindow.this.channels.setFileDescription(FileCommentWindow.this.fileCommentText.getText());
						}
					}
				});
			}
		}
	}
	
	public void update() {
		if (this.channels.getActiveChannel() != null) {
			this.fileCommentText.setText(this.channels.getFileDescription());
		}
	}
}