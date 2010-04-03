/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.dialog;

import java.io.IOException;
import java.util.jar.JarFile;
import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import osde.DE;
import osde.config.Settings;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.FileUtils;

/**
 * simple HTTP browser to display help info material
 * @author Winfried Brügmann
 */
public class HelpInfoDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger	log	= Logger.getLogger(HelpInfoDialog.class.getName());

	Shell		dialogShell;
	Browser	textBrowser;
	final Rectangle primaryMonitorBounds;
	final Settings	settings;


	public HelpInfoDialog(Shell parent, int style) {
		super(parent, style);
		this.primaryMonitorBounds = parent.getDisplay().getPrimaryMonitor().getBounds();
		this.settings = Settings.getInstance();
	}
	
	/**
	 * opens a fileURL of help HTML of the given device and pageName, 
	 * preferred style is SWT.MOZILLA which supports xulrunner on all platforms 
	 * @param deviceName
	 * @param fileName
	 * @param style
	 */
	public void open(String deviceName, String fileName, int style) {
		log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.dialogShell == null || this.dialogShell.isDisposed()) {
			this.dialogShell = new Shell(new Shell(Display.getDefault()), SWT.SHELL_TRIM);
			FillLayout dialogShellLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.dialogShell.setLayout(dialogShellLayout);
			this.dialogShell.setText(DE.OSDE_NAME_LONG + Messages.getString(MessageIds.OSDE_MSGT0192));
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/DataExplorer.jpg")); //$NON-NLS-1$

			this.textBrowser = new Browser(this.dialogShell, style);
			openURL(deviceName, fileName, style);

			this.dialogShell.layout();
			this.dialogShell.pack();
			int width = this.primaryMonitorBounds.width / 4 * 3;
			this.dialogShell.setSize(width, (this.primaryMonitorBounds.height * 95 / 100));
			this.dialogShell.setLocation(this.primaryMonitorBounds.width - width, 0);

			this.dialogShell.open();
		}
		else {
			this.dialogShell.setVisible(true);
			this.dialogShell.setActive();
			openURL(deviceName, fileName, style);
		}
		Display display = this.dialogShell.getDisplay();
		while (!this.dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	/**
	 * @param deviceName
	 * @param fileName
	 */
	private void openURL(String deviceName, String fileName, int style) {
		String jarBasePath = FileUtils.getOsdeJarBasePath() + "/";
		String jarName = "DataExplorer.jar";
		String helpDir = "help" + DE.FILE_SEPARATOR + this.settings.getLocale().getLanguage() + DE.FILE_SEPARATOR;
		String targetDir = DE.JAVA_IO_TMPDIR + (DE.IS_WINDOWS ? "" : DE.FILE_SEPARATOR) + "OSDE" + DE.FILE_SEPARATOR;
		
		if (deviceName.length() >= 1) { // devices/<deviceName>.jar
			jarBasePath = jarBasePath + "devices" + DE.FILE_SEPARATOR;
			jarName = deviceName + ".jar";
			targetDir = targetDir + deviceName + DE.FILE_SEPARATOR;
		}
		
		log.log(Level.FINE, "jarBasePath = " + jarBasePath + " jarName = " + jarName + " helpDir = " + helpDir); //$NON-NLS-1$
		
		try {
			FileUtils.extractDir(new JarFile(jarBasePath + jarName), helpDir, targetDir, "555");
			String stringUrl = targetDir + helpDir + fileName;
			log.log(Level.FINE, "stringUrl = " + "file:///" + stringUrl); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (style == SWT.MOZILLA) {
				this.textBrowser.setUrl("file:///" + stringUrl); //$NON-NLS-1$
			}
			else { // windows SWT.NONE
				this.textBrowser.setUrl(stringUrl);
			}
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialog(this.dialogShell, 
					Messages.getString(MessageIds.OSDE_MSGE0018, new Object[] { e.getLocalizedMessage() } )); //$NON-NLS-1$
		}
	}
	
	public boolean isDisposed() {
		return this.dialogShell != null ? this.dialogShell.isDisposed() : true;
	}
	
	public void dispose() {
		if (this.dialogShell != null) this.dialogShell.dispose();
	}
}
