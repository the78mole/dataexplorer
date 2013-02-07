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
    
    Copyright (c) 2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import java.util.logging.Logger;

import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Library;

/**
 * class to load IGC verification dll and execute the verification if the dll can be loaded
 * create header file:
 * /Users/brueg/Documents/workspace/gnu/WindowsHelper/IGCDLL>/usr/bin/javah -jni -classpath ../../IGCAdapter/classes gde.device.igc.IGCDLL
 *
 */
public class IGCDLL {
	private final static Logger	log	= Logger.getLogger(IGCDLL.class.getName());

	/**
	 * method to load the IGCDLL to verify the IGC file signature
	 * @param dllID identifier of the manufacturer XTT (Graupner HoTT Viewer)
	 * @return
	 */
	public static boolean loadIgcDll( String dllID) {
		boolean isLoaded = false;
		try {
			log.log(Level.INFO, "try to load IGC verification DLL: IGC-" + dllID + ".dll"); //$NON-NLS-1$
			Library.loadLibrary("IGC-" + dllID); //$NON-NLS-1$
			isLoaded = true;
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
			if (DataExplorer.getInstance().getMenuBar() != null) { //no UI is opened while running Junit test
				if (t instanceof UnsatisfiedLinkError)
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0037, new Object[] { t.getClass().getSimpleName() }) + Messages.getString(MessageIds.GDE_MSGI0033));
				else
					DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0038, new Object[] { t.getLocalizedMessage() }));
			}
		}
		return isLoaded;
	}

	/**
	 * native method called via load library to enable IGC DLL verification
	 * @param fqIgcFilePath to the IGC log file
	 * @return true|false
	 */
	public static native boolean validateLog(String fqIgcFilePath);

}
