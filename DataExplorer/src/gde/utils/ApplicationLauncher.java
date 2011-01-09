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
    
    Copyright (c) 2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import gde.GDE;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * class to execute a installed and registered application
 */
public class ApplicationLauncher {
	private final static Logger	log	= Logger.getLogger(ApplicationLauncher.class.getName());
	
	String fqExecPath = GDE.STRING_EMPTY;

	/**
	 * constructor where the path to the executable will be figured out if not given
	 */
	public ApplicationLauncher(String executable, String searchExecutableKey, String searchLocationInfo) {
		try {
			log.log(Level.FINE, "executeableName = '" + searchExecutableKey + GDE.STRING_SINGLE_QUOAT); //$NON-NLS-1$
			
			if (searchExecutableKey != null && searchExecutableKey.length() > 4) {
				searchExecutableKey = searchExecutableKey.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
				if (searchExecutableKey.contains(GDE.FILE_SEPARATOR_UNIX)) {
					searchExecutableKey = searchExecutableKey.substring(searchExecutableKey.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1);
				}

				if (GDE.IS_WINDOWS) {
					String path = WindowsHelper.findApplicationPath(searchExecutableKey);
					if (path.length() > 4) {
						path = path.substring(0, path.toLowerCase().indexOf(GDE.FILE_ENDING_EXE) + 4);
						if (path.startsWith("\"")) { //$NON-NLS-1$
							path = path.substring(1);
						}
						fqExecPath = path;
					}
				}
				else if (GDE.IS_LINUX) {
					List<String> command = new ArrayList<String>();
					command.add(Messages.getString(MessageIds.GDE_MSGT0602));
					command.add(searchExecutableKey);
					StringBuilder sb = new StringBuilder();
					Process process = new ProcessBuilder(command).start(); //$NON-NLS-1$ //$NON-NLS-2$
					process.waitFor();
					BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String line;
					while ((line = bisr.readLine()) != null) {
						log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
						sb.append(line);
					}
					while ((line = besr.readLine()) != null) {
						log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
					}
					if (process.exitValue() != 0) {
						String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
						log.log(Level.SEVERE, msg);
					}
					besr.close();
					bisr.close();
					
					if (sb.length() > 4) {
						fqExecPath = sb.toString();
					}
				}
				else if (GDE.IS_MAC) {				
					if (searchExecutableKey.toLowerCase().endsWith(GDE.STRING_MAC_DOT_APP)) {
						searchExecutableKey = searchExecutableKey.substring(0, searchExecutableKey.indexOf(GDE.STRING_MAC_DOT_APP));
					}
					String appDirectory = searchLocationInfo;
					if ((new File(appDirectory)).exists()) {
						String macExecPath = appDirectory + GDE.STRING_MAC_APP_EXE_PATH + searchExecutableKey;
						if (FileUtils.checkFileExist(macExecPath)) {
							fqExecPath = macExecPath;
						}
					}
				}
			}
			if (!fqExecPath.contains(executable)) {
				DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGT0603, new String[] { executable, searchLocationInfo, searchExecutableKey }));
			}
			else {
				if (GDE.IS_MAC) {
					fqExecPath = GDE.STRING_MAC_APP_OPEN;
				}
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(e.getMessage());
		}
	}
	
	/**
	 * execute the initialized executable with the given arguments
	 * @param arguments
	 */
	public void execute(final List<String> arguments) {
		if (this.isLaunchable()) {
		arguments.add(0, this.fqExecPath);
		if (GDE.IS_LINUX) {
			arguments.add("&"); //$NON-NLS-1$
		}
		for (String string : arguments) {
			log.log(Level.FINE, GDE.STRING_SINGLE_QUOAT + string + GDE.STRING_SINGLE_QUOAT);
		}
		Thread thread = new Thread() {
			public void run() {
				try {
					Process process = new ProcessBuilder(arguments).start(); //$NON-NLS-1$ //$NON-NLS-2$
					//process.waitFor();
					BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String line;
					while ((line = bisr.readLine()) != null) {
						log.log(Level.FINEST, "std.out = " + line); //$NON-NLS-1$
					}
					while ((line = besr.readLine()) != null) {
						log.log(Level.FINEST, "std.err = " + line); //$NON-NLS-1$
					}
					if (process.exitValue() != 0) {
						String msg = "failed to execute \"" + arguments + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
						log.log(Level.SEVERE, msg);
					}
					besr.close();
					bisr.close();
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					DataExplorer.getInstance().openMessageDialogAsync(e.getMessage());
				}
			}
		};
		thread.start();
	}
}
	/**
	 * @return true if full qualified file path length >= 4
	 */
	public boolean isLaunchable() {
		return this.fqExecPath.length() >= 4;
	}
}
