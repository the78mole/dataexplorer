package osde.device.conrad;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.data.Channels;
import osde.device.DeviceDialog;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Sample dialog implementation, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class VC800Dialog extends DeviceDialog {
	final static Logger						log	= Logger.getLogger(VC800Dialog.class.getName());

	Text													InfoText;
	Button												okButton;

	@SuppressWarnings("unused") //$NON-NLS-1$
	final VC800									device;																							// get device specific things, get serial port, ...
	@SuppressWarnings("unused") //$NON-NLS-1$
	final DeviceSerialPort				serialPort;																					// open/close port execute getData()....
	@SuppressWarnings("unused") //$NON-NLS-1$
	final OpenSerialDataExplorer	application;																					// interaction with application instance
	@SuppressWarnings("unused") //$NON-NLS-1$
	final Channels								channels;																						// interaction with channels, source of all records
	final Settings								settings;																						// application configuration settings

	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			VC800 device = new VC800("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Geraete\\Htronic Akkumaster C4.ini"); //$NON-NLS-1$
			VC800Dialog inst = new VC800Dialog(shell, device);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public VC800Dialog(Shell parent, VC800 useDevice) {
		super(parent);
		this.serialPort = useDevice.getSerialPort();
		this.device = useDevice;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			VC800Dialog.log.fine("dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254); 
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.layout();
				this.dialogShell.setText("Sample" + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273)); //$NON-NLS-1$
				this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.pack();
				this.dialogShell.setSize(336, 393);
				this.dialogShell.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent evt) {
						VC800Dialog.log.fine("dialogShell.focusGained, event=" + evt); //$NON-NLS-1$
						// this is only placed in the focus listener as hint, do not forget place this query 
						if (VC800Dialog.this.serialPort != null && !VC800Dialog.this.serialPort.isConnected()) {
							VC800Dialog.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0026));
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						VC800Dialog.log.fine("dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						//TODO check if some thing to do before exiting
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						VC800Dialog.log.fine("dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						VC800Dialog.this.application.openHelpDialog("Sample", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				// enable fade in/out alpha blending (do not fade-in on top)
				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent evt) {
						log.finer("dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, VC800Dialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
					}
					public void mouseHover(MouseEvent evt) {
						log.finest("dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}
					public void mouseExit(MouseEvent evt) {
						log.finer("dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, VC800Dialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
					}
				});
				{
					FormData InfoTextLData = new FormData();
					InfoTextLData.width = 304;
					InfoTextLData.height = 114;
					InfoTextLData.left = new FormAttachment(0, 1000, 12);
					InfoTextLData.top = new FormAttachment(0, 1000, 81);
					this.InfoText = new Text(this.dialogShell, SWT.MULTI | SWT.CENTER | SWT.WRAP);
					this.InfoText.setLayoutData(InfoTextLData);
					this.InfoText.setText(Messages.getString(MessageIds.OSDE_MSGW1001));
					this.InfoText.setFont(SWTResourceManager.getFont("Sans Serif", 12, SWT.BOLD, false, false)); //$NON-NLS-1$
					this.InfoText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
					// enable fade in for big areas inside the dialog while fast mouse move
					this.InfoText.addMouseTrackListener(VC800Dialog.this.mouseTrackerEnterFadeOut);
				}
				{
					FormData okButtonLData = new FormData();
					okButtonLData.width = 52;
					okButtonLData.height = 33;
					okButtonLData.left = new FormAttachment(0, 1000, 132);
					okButtonLData.top = new FormAttachment(0, 1000, 309);
					this.okButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.okButton.setLayoutData(okButtonLData);
					this.okButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0188));
					this.okButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							VC800Dialog.log.finest("okButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							VC800Dialog.this.close();
						}
					});
				}
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-175, 100));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			VC800Dialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
