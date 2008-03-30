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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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

	private Logger					log	= Logger.getLogger(this.getClass().getName());

	private TabItem					commentTab;
	private Composite				commentMainComposite;
	private CLabel 					infoLabel;
	private Text						fileCommentText;

	private final Channels	channels;
	private final TabFolder	displayTab;

	/**
	 * constructor with TabFolder parent
	 * @param displayTab
	 */
	public FileCommentWindow(TabFolder displayTab) {
		this.displayTab = displayTab;
		this.channels = Channels.getInstance();
	}

	/**
	 * method to create the window and register required event listener
	 */
	public void create() {
		commentTab = new TabItem(displayTab, SWT.NONE);
		commentTab.setText("Dateikommentar");
		SWTResourceManager.registerResourceUser(commentTab);

		{
			commentMainComposite = new Composite(displayTab, SWT.NONE);
			commentTab.setControl(commentMainComposite);
			commentMainComposite.setLayout(null);
			commentMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("cellVoltageMainComposite.paintControl, event=" + evt);
					Point mainSize = commentMainComposite.getSize();
					//log.info("mainSize = " + mainSize.toString());
					Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
							, mainSize.x * 90/100, mainSize.y * 80/100);
					//log.info("cover bounds = " + bounds.toString());
					infoLabel.setBounds(50, 10, bounds.width, bounds.y-10);
					fileCommentText.setBounds(bounds);
				}
			});
			{
				infoLabel = new CLabel(commentMainComposite, SWT.LEFT);
				infoLabel.setText("Dateikommentar");
				infoLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
				infoLabel.setBounds(50, 10, 500, 26);
			}
			commentMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.finer("commentMainComposite.paintControl, event=" + evt);
					fileCommentText.setText(channels.getFileDescription());
				}
			});
			{
				fileCommentText = new Text(commentMainComposite, SWT.LEFT | SWT.TOP | SWT.MULTI | SWT.WRAP | SWT.BORDER);
				fileCommentText.setText("Dateikommentar : ");
				fileCommentText.setBounds(50, 40, 500, 300);
				fileCommentText.setText(channels.getFileDescription());
				fileCommentText.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.finer("fileCommentText.helpRequested");
						OpenSerialDataExplorer.getInstance().openHelpDialog("OpenSerialDataExplorer", "HelpInfo_10.html");
					}
				});
				fileCommentText.addKeyListener(new KeyListener() {
					public void keyPressed(KeyEvent evt) {
						log.finest("recordSelectCombo.keyPressed, event=" + evt);
						if (evt.character == SWT.CR) {
								channels.setFileDescription(fileCommentText.getText());
						}
					}
					public void keyReleased(KeyEvent evt) {
					}
				});
			}
		}
	}
	
	public void update() {
		if (channels.getActiveChannel() != null) {
			fileCommentText.setText(channels.getFileDescription());
		}
	}
}