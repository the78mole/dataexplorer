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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import osde.data.Channels;

/**
 * Class to enable a file comment
 * @author Winfried Brügmann
 */
public class FileCommentWindow {
	private Logger					log	= Logger.getLogger(this.getClass().getName());

	private TabItem					commentTab;
	private Composite				commentMainComposite;
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
		{
			commentMainComposite = new Composite(displayTab, SWT.NONE);
			FormLayout fileCommentCompositeLayout = new FormLayout();
			commentTab.setControl(commentMainComposite);
			commentMainComposite.setLayout(fileCommentCompositeLayout);
			{
				fileCommentText = new Text(commentMainComposite, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
				fileCommentText.setText("Dateikommentar : ");
				FormData fileCommentTextLData = new FormData();
				fileCommentTextLData.left = new FormAttachment(0, 1000, 30);
				fileCommentTextLData.top = new FormAttachment(0, 1000, 30);
				fileCommentTextLData.right = new FormAttachment(1000, 1000, -30);
				fileCommentTextLData.bottom = new FormAttachment(1000, 1000, -30);
				fileCommentText.setLayoutData(fileCommentTextLData);
				fileCommentText.setText(channels.getFileDescription());
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
}