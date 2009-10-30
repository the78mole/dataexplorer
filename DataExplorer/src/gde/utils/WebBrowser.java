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
package osde.utils;

import java.io.IOException;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.config.Settings;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * This is a simple web browser launcher utility, where help pages can be displayed
 * @author Winfried Brügmann
 */
public class WebBrowser {
	private static Logger				log			= Logger.getLogger(WebBrowser.class.getName());

	public static void openURL(String deviceName, String fileName) {
		String basePath = FileUtils.getOsdeJarBasePath() + "/";
		String jarName = "OpenSerialDataExplorer.jar";
		
		if (deviceName.length() >= 1) { // devices/<deviceName>.jar
			basePath = basePath + "devices/";
			jarName = deviceName + ".jar";
		}
		
		log.log(Level.FINE, "basePath = " + basePath + " jarName = " + jarName); //$NON-NLS-1$
		
		try {
			String targetDir = OSDE.JAVA_IO_TMPDIR + "OSDE" + OSDE.FILE_SEPARATOR;
			String helpDir = "help" + OSDE.FILE_SEPARATOR + Settings.getInstance().getLocale().getLanguage() + OSDE.FILE_SEPARATOR;
			FileUtils.extractDir(new JarFile(basePath + jarName), helpDir, targetDir, "555");
			
			String stringUrl = targetDir + helpDir + fileName;
			log.log(Level.FINE, "stringUrl = " + stringUrl); //$NON-NLS-1$

			openBrowser(stringUrl);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog(
					Messages.getString(MessageIds.OSDE_MSGE0018, new Object[] { e.getLocalizedMessage() } )); //$NON-NLS-1$
		}
	}

	/**
	 * @param stringUrl
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static void openBrowser(String stringUrl) {
		try {
			if (OSDE.IS_WINDOWS) {
				Runtime.getRuntime().exec("rundll32.exe url.dll,FileProtocolHandler " + stringUrl); //$NON-NLS-1$
			}
			else if (OSDE.IS_LINUX){
				String[] browsers = { "firefox", "konqueror", "opera", "epiphany", "mozilla", "netscape" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++)
					if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) browser = browsers[count]; //$NON-NLS-1$

				if (browser == null)
					throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0019, new Object[]
							{ "firefox, konqueror, opera, epiphany, mozilla, netscape" } )); //$NON-NLS-1$
				
				Runtime.getRuntime().exec(browser + OSDE.STRING_BLANK + stringUrl);
			}
			else if (OSDE.IS_MAC) {
		 		Runtime.getRuntime().exec("open" + OSDE.STRING_BLANK + stringUrl);
		 }
			else {
				throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0020, new Object[] {System.getProperty(OSDE.STRING_OS_NAME)} )); 
			}

		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.OSDE_MSGE0021, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
	}
}
