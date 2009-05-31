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

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;


/**
 * @author Winfried Brügmann
 * helper class to create a windows shell link using JNI to access native windows OLE functions in WinHelper library 
 */
public class WindowsHelper {
	private final static Logger	log	= Logger.getLogger(WindowsHelper.class.getName());

	static {
		try {
			//using internal load functionality where the library may packed within the jar
			String prop = System.getProperty("sun.arch.data.model"); //$NON-NLS-1$
			if (prop == null) prop = System.getProperty("com.ibm.vm.bitmode"); //$NON-NLS-1$

			log.log(Level.INFO, "bitmode = " + prop); //$NON-NLS-1$
			Library.loadLibrary( "WinHelper" + prop ); //$NON-NLS-1$
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
			if (t instanceof UnsatisfiedLinkError)
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0037, new Object[] {t.getClass().getSimpleName()}) 
						+ Messages.getString(MessageIds.OSDE_MSGI0033));
			else
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0038, new Object[] {t.getLocalizedMessage()}));
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String [] shellLinkArgs = {
				"C:\\Documents and Settings\\brueg\\Desktop\\OpenSerialDataExplorer.lnk", //%USERPROFILE% //$NON-NLS-1$
				"%WINDIR%\\system32\\javaw.exe", //$NON-NLS-1$
				"-jar -Xms40M -Xmx256M \"C:\\Program Files\\OpenSerialDataExplorer\\OpenserialDataExplorer.jar\"", //$NON-NLS-1$
				"C:\\Program Files\\OpenSerialDataExplorer", //$NON-NLS-1$
				"C:\\Program Files\\OpenSerialDataExplorer\\OpenSerialDataExplorer.ico",  //$NON-NLS-1$
				"OpenSerialData Explorer" }; //$NON-NLS-1$
		
		createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);
	}

	/**
	 * native method called via load library to enable use of native windows ole32 functions
	 * @param fqShellLinkPath
	 * @param fqExecutablePath
	 * @param executableArguments
	 * @param workingDirectory
	 * @param fqIconPath
	 * @param iconPosition
	 * @param description
	 */
	public static native void createDesktopLink(
			String fqShellLinkPath,
			String fqExecutablePath,
			String executableArguments,
			String workingDirectory, 
			String fqIconPath, 
			int 	 iconPosition,
			String description
		);

	/**
	 * native method called via load library to enable use of native windows ole32 functions
	 * @param fqShellLinkPath
	 * @return contained full qualified file path
	 */
	public static native String getFilePathFromLink(String fqShellLinkPath);

	/**
	 * native method called via load library to enable use of native windows functions to register OpenSerialData MIME type to associate .osd
	 * @param applicationInstallationDirectory
	 */
	//public static native String registerMimeType(	String applicationInstallationDirectory	) throws SecurityException, IOException;

	/**
	 * native method called via load library to enable use of native windows functions to remove file type association
	 */
	//public static native String deregisterMimeType() throws SecurityException, IOException;
}
