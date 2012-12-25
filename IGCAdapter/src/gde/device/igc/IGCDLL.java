package gde.device.igc;

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
			if (t instanceof UnsatisfiedLinkError)
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0037, new Object[] { t.getClass().getSimpleName() }) + Messages.getString(MessageIds.GDE_MSGI0033));
			else
				DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0038, new Object[] { t.getLocalizedMessage() }));
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
