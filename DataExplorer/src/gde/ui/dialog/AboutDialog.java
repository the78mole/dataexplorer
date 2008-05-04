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
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.jpg"));
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();			
			this.dialogShell.setSize(650, 430);
			this.dialogShell.setText("About Dialog");
			{
				FormData infoTextLData = new FormData();
				infoTextLData.width = 610;
				infoTextLData.height = 240;
				infoTextLData.left =  new FormAttachment(0, 1000, 20);
				infoTextLData.top =  new FormAttachment(0, 1000, 111);
				infoTextLData.right =  new FormAttachment(1000, 1000, -20);
				this.infoText = new Text(this.dialogShell, SWT.LEFT | SWT.MULTI | SWT.WRAP);
				this.infoText.setLayoutData(infoTextLData);
				this.infoText.setText("Der OpenSerialDataExplorer ist ein Werkzeug zu aufnehmen, betrachten und auswerten von Daten aus Geräten, die über eine serielle Schnittstelle erreichbar sind. Geräte könne Datenlogger, Messgeräte, Ladegeräte oder ähnliches sein."
						+ System.getProperty("line.separator") + "Open Serial Data Explorer benutzt für die serielle Kommunikation die freie Implementierung des unter GPL stehenden RXTXComm Paketes und die grafische Benutzeroberfläche (GUI) basiert auf dem aus der Eclipse Welt kommenden Software Widged Toolkit (SWT)." 
						+ System.getProperty("line.separator") + "Die Software wurde sorgfältig getestet. Trotzdem könnte es durch Fehlbedienung der angeschlossenen Geräte zu beschädigungen kommen. Die Benutung erfolgt auf eigenes Risiko, ein Schadensersatz wir in keinem Fall geleistet." 
						+ System.getProperty("line.separator") + "Bei der Benutzung können dem Anwender Fehler auffallen oder Verbesserungsvorschläge einfallen. Über einen Mitteilung solcher Umstände bin ich dankbar (winfried@bruegmaenner.de). Eine umgehende Beseitigung von Fehlern oder das Einbauen von Vergesserungen kann aber nicht garantiert werden, da es sich hier um ein reines Freizeitprojekt handelt.");
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
				this.ok.setText("OK");
				this.ok.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("ok.widgetSelected, event="+evt);
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
				this.aboutText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 18, 2, false, false));
				this.aboutText.setText("Open Serial Data Explorer");
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
