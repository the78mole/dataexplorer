package osde.device.smmodellbau;

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

import osde.data.Channels;
import osde.device.DeviceDialog;
import osde.device.DeviceSerialPort;
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
public class UniLogDialog extends DeviceDialog {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private Shell													dialogShell;
	private Text													InfoText;
	private Button												okButton;

	@SuppressWarnings("unused")
	private final UniLog						device;				// get device specific things, get serial port, ...
	@SuppressWarnings("unused")
	private final DeviceSerialPort				serialPort; 	// open/close port execute getData()....
	@SuppressWarnings("unused")
	private final OpenSerialDataExplorer	application;	// interaction with application instance
	@SuppressWarnings("unused")
	private final Channels								channels;			// interaction with channels, source of all records

	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			UniLog device = new UniLog("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Geraete\\Htronic Akkumaster C4.ini");
			UniLogDialog inst = new UniLogDialog(shell, device);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param device device specific class implementation
	 */
	public UniLogDialog(Shell parent, UniLog device) {
		super(parent);
		this.serialPort = device.getSerialPort();
		this.device = device;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	public void open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM); // !SWT.APPLICATION_MODAL
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(336, 393);
			{
				FormData InfoTextLData = new FormData();
				InfoTextLData.width = 304;
				InfoTextLData.height = 114;
				InfoTextLData.left = new FormAttachment(0, 1000, 12);
				InfoTextLData.top = new FormAttachment(0, 1000, 81);
				InfoText = new Text(dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
				InfoText.setLayoutData(InfoTextLData);
				InfoText.setText("Für diese Gerät ist die Kommunikationsschnittstelle nicht implementiert.");
				InfoText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
				InfoText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
			}
			{
				FormData okButtonLData = new FormData();
				okButtonLData.width = 52;
				okButtonLData.height = 33;
				okButtonLData.left = new FormAttachment(0, 1000, 132);
				okButtonLData.top = new FormAttachment(0, 1000, 309);
				okButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				okButton.setLayoutData(okButtonLData);
				okButton.setText("OK");
				okButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("okButton.widgetSelected, event=" + evt);
						dialogShell.dispose();
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
