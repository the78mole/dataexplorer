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
package osde.ui.dialog;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class showing some info text with disclaimers, version , ...
 * @author Winfried Brügmann
 */
public class AboutDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger log = Logger.getLogger(AboutDialog.class.getName());

	Shell dialogShell;
	private Text aboutText;
	private Button ok;
	private Text infoText;
	private Text version;

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			AboutDialog inst = new AboutDialog(shell, SWT.NULL);
			inst.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AboutDialog(Shell parent, int style) {
		super(parent, style);
	}

	public void open() {
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.jpg")); //$NON-NLS-1$
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();			
			this.dialogShell.setSize(650, 430);
			this.dialogShell.setText(Messages.getString(MessageIds.OSDE_MSGT0146));
			{
				FormData infoTextLData = new FormData();
				infoTextLData.width = 610;
				infoTextLData.height = 240;
				infoTextLData.left =  new FormAttachment(0, 1000, 20);
				infoTextLData.top =  new FormAttachment(0, 1000, 111);
				infoTextLData.right =  new FormAttachment(1000, 1000, -20);
				this.infoText = new Text(this.dialogShell, SWT.LEFT | SWT.MULTI | SWT.WRAP);
				this.infoText.setLayoutData(infoTextLData);
				this.infoText.setText(Messages.getString(MessageIds.OSDE_MSGT0147)
						+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGT0148)  //$NON-NLS-1$ 
						+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGT0149)  //$NON-NLS-1$
						+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGT0150)); //$NON-NLS-1$
				this.infoText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
			}
			{
				FormData versionLData = new FormData();
				versionLData.width = 610;
				versionLData.height = 25;
				versionLData.left =  new FormAttachment(0, 1000, 20);
				versionLData.top =  new FormAttachment(0, 1000, 72);
				versionLData.right =  new FormAttachment(1000, 1000, -20);
				this.version = new Text(this.dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
				this.version.setLayoutData(versionLData);
				this.version.setText(OSDE.OSDE_VERSION);
				this.version.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
			}
			{
				FormData okLData = new FormData();
				okLData.width = 40;
				okLData.height = 35;
				okLData.left =  new FormAttachment(0, 1000, 147);
				okLData.bottom =  new FormAttachment(1000, 1000, -12);
				okLData.right =  new FormAttachment(1000, 1000, -154);
				this.ok = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.ok.setLayoutData(okLData);
				this.ok.setText("OK"); //$NON-NLS-1$
				this.ok.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("ok.widgetSelected, event="+evt); //$NON-NLS-1$
						AboutDialog.this.dialogShell.dispose();
					}
				});
			}
			{
				FormData aboutTextLData = new FormData();
				aboutTextLData.width = 593;
				aboutTextLData.height = 39;
				aboutTextLData.left =  new FormAttachment(0, 1000, 20);
				aboutTextLData.top =  new FormAttachment(0, 1000, 21);
				aboutTextLData.right =  new FormAttachment(1000, 1000, -20);
				this.aboutText = new Text(this.dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
				this.aboutText.setLayoutData(aboutTextLData);
				this.aboutText.setFont(SWTResourceManager.getFont("Sans Serif", 18, 2, false, false)); //$NON-NLS-1$
				this.aboutText.setText("Open Serial Data Explorer"); //$NON-NLS-1$
				this.aboutText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				this.aboutText.setText(OpenSerialDataExplorer.getInstance().getClass().getSimpleName());
			}
			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
}
