package osde.ui.dialog;
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

import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class AboutDialog extends org.eclipse.swt.widgets.Dialog {

	private Shell dialogShell;
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
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.jpg"));

			{
				//Register as a resource user - SWTResourceManager will
				//handle the obtaining and disposing of resources
				SWTResourceManager.registerResourceUser(dialogShell);
			}
			

			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.setSize(650, 430);
			dialogShell.setBackgroundImage(SWTResourceManager.getImage("osde/resource/OpenSerialDataExplorer.jpg"));
			dialogShell.setText("OSDE - About");
			//dialogShell.setBackground(osde.COLOR_LIGHT_GREY);
			{
				FormData infoTextLData = new FormData();
				infoTextLData.width = 610;
				infoTextLData.height = 240;
				infoTextLData.left =  new FormAttachment(0, 1000, 20);
				infoTextLData.top =  new FormAttachment(0, 1000, 111);
				infoTextLData.right =  new FormAttachment(1000, 1000, -20);
				infoText = new Text(dialogShell, SWT.MULTI | SWT.WRAP);
				infoText.setLayoutData(infoTextLData);
				infoText.setText("Open Serial Data Explorer (OSDE) benutzt für die serielle Kommunikation die freie Implementierung des unter GPL stehenden RXTXComm Paketes und die grafische Benutzeroberfläche (GUI) basiert auf dem aus der Eclipse Welt kommenden Software Widged Toolkit (SWT)." 
						+ System.getProperty("line.separator") + "Die Software OSDE wurde sorgfältig getestet. Trotzdem kann es durch Fehlbedienung der angeschlossenen Geräte zu beschädigungen kommen. Die Benutung von OSDE erfolgt deshalb auf eigenes Risiko, ein Schadensersatz wir in keinem Fall geleistet." 
						+ System.getProperty("line.separator") + "Bei der Benutzung können dem Anwender Fehler auffallen oder Verbesserungsvorschläge einfallen. Über einen Mitteilung solcher Umstände bin ich dankbar. Eine umgehende Beseitigung von Fehlern oder das Einbauen von Vergesserungen kann aber nicht garantiert werden, da es sich hier um ein Freizeitprojekt handelt.");
				//infoText.setBackground(OpenSerielDataExplorer.COLOR_VERY_LIGHT_GREY);
			}
			{
				FormData versionLData = new FormData();
				versionLData.width = 610;
				versionLData.height = 25;
				versionLData.left =  new FormAttachment(0, 1000, 20);
				versionLData.top =  new FormAttachment(0, 1000, 72);
				versionLData.right =  new FormAttachment(1000, 1000, -20);
				version = new Text(dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
				version.setLayoutData(versionLData);
				version.setText("Version 0.4");
				version.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
			}
			{
				FormData okLData = new FormData();
				okLData.width = 40;
				okLData.height = 35;
				okLData.left =  new FormAttachment(0, 1000, 147);
				okLData.bottom =  new FormAttachment(1000, 1000, -12);
				okLData.right =  new FormAttachment(1000, 1000, -154);
				ok = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				ok.setLayoutData(okLData);
				ok.setText("OK");
				ok.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("ok.widgetSelected, event="+evt);
						dialogShell.dispose();
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
				aboutText = new Text(dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
				aboutText.setLayoutData(aboutTextLData);
				aboutText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 18, 2, false, false));
				aboutText.setText("Open Serial Data Explorer");
				//aboutText.setBackground(OpenSerielDataExplorer.COLOR_LIGHT_GREY);
				aboutText.setText(OpenSerialDataExplorer.getInstance().getClass().getSimpleName());
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
