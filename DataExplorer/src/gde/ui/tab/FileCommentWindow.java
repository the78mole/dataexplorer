/**
 * 
 */
package osde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import osde.common.Channels;

/**
 * @author winfried bruegmann
 *
 */
public class FileCommentWindow {
	private Logger					log	= Logger.getLogger(this.getClass().getName());

	private TabItem					commentTab;
	private Composite				commentMainComposite;
	private Text						fileCommentText;

	private final Channels	channels;
	private final TabFolder	displayTab;

	public FileCommentWindow(TabFolder displayTab) {
		this.displayTab = displayTab;
		this.channels = Channels.getInstance();
	}

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
				//fileCommentTextLData.width = 722;
				//fileCommentTextLData.height = 408;
				fileCommentTextLData.left = new FormAttachment(0, 1000, 30);
				fileCommentTextLData.top = new FormAttachment(0, 1000, 30);
				fileCommentTextLData.right = new FormAttachment(1000, 1000, -30);
				fileCommentTextLData.bottom = new FormAttachment(1000, 1000, -30);
				fileCommentText.setLayoutData(fileCommentTextLData);
				fileCommentText.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.finest("analogMainComposite.paintControl, event=" + evt);
							fileCommentText.setText(channels.getFileDescription());
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
}