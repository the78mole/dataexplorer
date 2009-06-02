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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * Utility class with helpers around operating system
 * @author Winfried Brügmann
 */
public class OperatingSystemHelper {
	private static final Logger	log			= Logger.getLogger(OperatingSystemHelper.class.getName());
	
	/**
	 * create destop shortcut to launch the main jar
	 */
	public static boolean createDesktopLink() {
		boolean isCreated = false;
		URL url = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();
		String sourceLaunchFilePath, targetDesktopLaucherFilePath;
		String sourceBasePath = url.getPath(), targetBasePath;

		if (url.getPath().endsWith(OSDE.FILE_SEPARATOR_UNIX)) { // running inside Eclipse
			log.log(Level.INFO, "started inside Eclipse -> skip creation of shortcut"); //$NON-NLS-1$
		}
		else {
			log.log(Level.INFO, "started outside with: java -jar *.jar"); //$NON-NLS-1$

			if (OSDE.IS_WINDOWS) {
				try {
					String launchFilename = "OpenSerialDataExplorer.exe"; //$NON-NLS-1$
					sourceBasePath = sourceBasePath.substring(1, sourceBasePath.lastIndexOf(OSDE.FILE_SEPARATOR_UNIX) + 1).replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK); //$NON-NLS-1$ //$NON-NLS-2$
					log.log(Level.INFO, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$
					sourceLaunchFilePath = (sourceBasePath + launchFilename);
					log.log(Level.INFO, "sourceLaunchFilePath = " + sourceLaunchFilePath); //$NON-NLS-1$
					targetBasePath = System.getenv("USERPROFILE") + OSDE.FILE_SEPARATOR_UNIX + "Desktop" + OSDE.FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
					targetDesktopLaucherFilePath = targetBasePath + "OpenSerialData Explorer.lnk"; //$NON-NLS-1$
					log.log(Level.INFO, "fqShellLinkPath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
					String fqExecutablePath = sourceLaunchFilePath.replace("/", OSDE.FILE_SEPARATOR); //$NON-NLS-1$
					log.log(Level.INFO, "fqExecutablePath = " + fqExecutablePath); //$NON-NLS-1$
					String executableArguments = ""; //exe wrapper dont need arguments - "-jar -Xms40M -Xmx256M \"" + sourceLaunchFilePath + "jar\""; //$NON-NLS-1$
					log.log(Level.INFO, "executableArguments = " + executableArguments); //$NON-NLS-1$
					String workingDirectory = sourceBasePath.replace("/", OSDE.FILE_SEPARATOR); //$NON-NLS-1$
					log.log(Level.INFO, "workingDirectory = " + workingDirectory); //$NON-NLS-1$
					String fqIconPath = fqExecutablePath; // exe wrapper will contain icon - sourceLaunchFilePath + "ico";
					log.log(Level.INFO, "fqIconPath = " + fqIconPath); //$NON-NLS-1$
					String description = Messages.getString(MessageIds.OSDE_MSGT0400);
					log.log(Level.INFO, "description = " + description); //$NON-NLS-1$

					String[] shellLinkArgs = { targetDesktopLaucherFilePath, fqExecutablePath, executableArguments, workingDirectory, fqIconPath, description };

					WindowsHelper.createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);
					isCreated = true;
				}
				catch (Throwable e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
			else if (OSDE.IS_LINUX) { //$NON-NLS-1$
				try {
					sourceBasePath = sourceBasePath.substring(0, sourceBasePath.lastIndexOf(OSDE.FILE_SEPARATOR_UNIX)+1);
					log.log(Level.INFO, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$
					
					String desktopFileName = "OpenSerialDataExplorer.desktop"; //$NON-NLS-1$
					String extractTargetFilePath = sourceBasePath+desktopFileName;
					log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
					File targetFile = new File(extractTargetFilePath);
					
					//installation directory must contain OpenSerialDataExplorer.desktop with write permission
					if (targetFile.exists()  && targetFile.canWrite()) {
						String jarFilePath = sourceBasePath + "OpenSerialDataExplorer.jar"; //$NON-NLS-1$
						log.log(Level.INFO, "jarFilePath = " + jarFilePath); //$NON-NLS-1$
						
						FileUtils.extractWhileReplace("@OSDE_DIR@", sourceBasePath, jarFilePath, desktopFileName, extractTargetFilePath, OSDE.STRING_UTF_8, OSDE.STRING_UTF_8); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

						targetBasePath = System.getenv("HOME") + OSDE.FILE_SEPARATOR_UNIX + "Desktop" + OSDE.FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
						log.log(Level.INFO, "targetBasePath = " + targetBasePath); //$NON-NLS-1$
						targetDesktopLaucherFilePath = targetBasePath + desktopFileName;
						log.log(Level.INFO, "targetDesktopLaucherFilePath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
						FileUtils.copyFile(new File(extractTargetFilePath), new File(targetDesktopLaucherFilePath));
						
						isCreated = true;
					}
					else {
						log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, a desktop launcher can not created"); //$NON-NLS-1$
					}
				}
				catch (UnsupportedEncodingException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				catch (FileNotFoundException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage());
				}
				catch (Throwable e) {
					log.log(Level.WARNING, e.getMessage());
				}
			}
			else {
				log.log(Level.WARNING, "not supported OS"); //$NON-NLS-1$
				OpenSerialDataExplorer.getInstance().openMessageDialog("Operating System " + System.getProperty(OSDE.STRING_OS_NAME) + " is not supported!");
			}
		}
		log.log(Level.INFO, "OpenSerialDataExplorer desktop created = " + isCreated); //$NON-NLS-1$
		return isCreated;
	}
	
	/**
	 * remove destop shortcut to launch the main jar
	 */
	public static boolean removeDesktopLink() {
		boolean isRemoved = false;
		String targetBasePath, targetDesktopLaucherFilePath;

		try {
			if (OSDE.IS_WINDOWS) {
				targetBasePath = System.getenv("USERPROFILE") + OSDE.FILE_SEPARATOR_WINDOWS + "Desktop" + OSDE.FILE_SEPARATOR_WINDOWS; //$NON-NLS-1$ //$NON-NLS-2$
				targetDesktopLaucherFilePath = targetBasePath + "OpenSerialData*.lnk"; //$NON-NLS-1$
				log.log(Level.INFO, "fqShellLinkPath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$

				Process process = Runtime.getRuntime().exec("cmd /C erase /F \"" + targetDesktopLaucherFilePath + "\""); //$NON-NLS-1$
				process.waitFor();
				if (process.exitValue() != 0) {
					log.log(Level.WARNING, "failed to remove desktop launcher"); //$NON-NLS-1$
				}
				isRemoved = true;
			}
			else if (OSDE.IS_LINUX) {
				String desktopFileName = "OpenSerialDataExplorer.desktop"; //$NON-NLS-1$
				targetBasePath = System.getenv("HOME") + OSDE.FILE_SEPARATOR_UNIX + "Desktop" + OSDE.FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
				log.log(Level.INFO, "targetBasePath = " + targetBasePath); //$NON-NLS-1$
				targetDesktopLaucherFilePath = targetBasePath + desktopFileName;
				log.log(Level.INFO, "targetDesktopLaucherFilePath = " + targetDesktopLaucherFilePath); //$NON-NLS-1$
				
				Process process = Runtime.getRuntime().exec("rm -f " + targetDesktopLaucherFilePath); //$NON-NLS-1$
				process.waitFor();
				if (process.exitValue() != 0) {
					log.log(Level.WARNING, "failed to remove desktop launcher"); //$NON-NLS-1$
				}
				isRemoved = true;
			}
			else {
				OpenSerialDataExplorer.getInstance().openMessageDialog("Remove desktopLink for Operating System " + System.getProperty(OSDE.STRING_OS_NAME) + " is not supported!");
			}
		}
		catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage());
		}
		return isRemoved;
	}

	/**
	 * register the application to the running OS
	 * associate file ending .osd as shell support
	 * @return true for successful registration
	 */
	public static boolean registerApplication() {
		int rc = -1;
		String targetDir = OSDE.JAVA_IO_TMPDIR;
		String command = OSDE.STRING_BLANK;
		
		try {
			URL url = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();
			
			if (url.getPath().endsWith(OSDE.FILE_SEPARATOR_UNIX)) { // running inside Eclipse
				log.log(Level.INFO, "started inside Eclipse -> skip creation of shortcut"); //$NON-NLS-1$
			}
			else {
				log.log(Level.INFO, "started outside with: java -jar *.jar"); //$NON-NLS-1$

				String jarBasePath = FileUtils.getOsdeJarBasePath();
				String jarFilePath = jarBasePath + "/OpenSerialDataExplorer.jar"; //$NON-NLS-1$

				JarFile jarFile = new JarFile(jarFilePath);

				if (OSDE.IS_WINDOWS) { 
					// warn user for UAC or fail due to required admin rights accessing registry
					OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0029));
					String regExe = "Register" + OSDE.BIT_MODE + ".exe"; //$NON-NLS-1$ //$NON-NLS-2$ 
					log.log(Level.INFO, "register exe = " + regExe); //$NON-NLS-1$	

					FileUtils.extract(jarFile, regExe, OSDE.STRING_EMPTY, targetDir, "WIN"); //$NON-NLS-1$
					String targetBasePath = jarBasePath.replace(OSDE.FILE_SEPARATOR_UNIX, OSDE.FILE_SEPARATOR_WINDOWS);
					targetBasePath = targetBasePath.startsWith(OSDE.FILE_SEPARATOR_WINDOWS) ? targetBasePath.substring(1) : targetBasePath;
					targetBasePath = targetBasePath.endsWith(OSDE.FILE_SEPARATOR_WINDOWS) ? targetBasePath.substring(0, targetBasePath.length()-1) : targetBasePath;
					command = "cmd /C " + targetDir + regExe + OSDE.STRING_BLANK + "\"" + targetBasePath + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$  
					log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$	
					Process process = Runtime.getRuntime().exec(command);
					process.waitFor();
					BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String line;

					StringBuilder sb = new StringBuilder();
					while ((line = bisr.readLine()) != null) {
						sb.append(line);
					}
					log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
					sb = new StringBuilder();
					while ((line = besr.readLine()) != null) {
						sb.append(line);
					}
					log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
					if (process.exitValue() != 0) {
						String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-1$
						log.log(Level.SEVERE, msg);
						if (msg.contains("740"))
							throw new IOException("error=740");

						throw new UnsatisfiedLinkError(msg);
					}

					//check if registration was successful
					command = "cmd /C assoc .osd"; //$NON-NLS-1$
					log.log(Level.INFO, "executing \"" + command + "\" to check association"); //$NON-NLS-1$ //$NON-NLS-2$
					process = Runtime.getRuntime().exec(command);
					process.waitFor();
					bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
					besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

					sb = new StringBuilder();
					while ((line = bisr.readLine()) != null) {
						sb.append(line);
					}
					log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
					sb = new StringBuilder();
					while ((line = besr.readLine()) != null) {
						sb.append(line);
					}
					log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
					log.log(Level.INFO, "\"" + command + "\" rc = " + process.exitValue()); //$NON-NLS-1$ //$NON-NLS-2$
					if (process.exitValue() != 0) {
						log.log(Level.WARNING, "failed to register OpenSerialData MIME type rc = " + process.exitValue()); //$NON-NLS-1$
						throw new IOException("error=740");
					}
					rc = 0;
				}
				else if (OSDE.IS_LINUX) { 
					String desktopFileName = "OpenSerialDataExplorer.desktop"; //$NON-NLS-1$
					String extractTargetFilePath = jarBasePath + desktopFileName;
					log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
					File targetFile = new File(extractTargetFilePath);

					//installation directory must contain OpenSerialDataExplorer.desktop with write permission
					if (targetFile.exists() && targetFile.canWrite()) {
						FileUtils.extractWhileReplace("@OSDE_DIR@", jarBasePath, jarFilePath, desktopFileName, extractTargetFilePath, "UTF-8", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$);
						FileUtils.extract(jarFile, "register.sh", "", targetDir, "555"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						FileUtils.extract(jarFile, "OpenSerialDataExplorer.directory", "", targetDir, "555"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

						command = "chmod +x " + targetDir + "/register.sh"; //$NON-NLS-1$ //$NON-NLS-2$
						log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
						Runtime.getRuntime().exec(command).waitFor();
						command = targetDir + "/register.sh"; //$NON-NLS-1$
						log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
						rc = Runtime.getRuntime().exec(command).waitFor();
					}
					else {
						// package error, must not occur in a deliverd driver
						log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, the OpenSerialDataExplorer MIME-type can not registered"); //$NON-NLS-1$
					}

					//check if xdg-utls are installed, this is the prerequisite for the registration process				
					if (Runtime.getRuntime().exec("which xdg-mime").waitFor() != 0) { //$NON-NLS-1$
						log.log(Level.INFO, "OpenSerialData program can not registered until xdg-utils are installed and in path"); //$NON-NLS-1$
						OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0030));
						rc = 0;
					}
				}
				else if (OSDE.IS_MAC) { //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0031));
					rc = 0;
				}
				else {
					log.log(Level.INFO, "Unsupported OS, shell integration, MIME registration NOT IMPLEMENTED"); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0024, new Object[] {System.getProperty(OSDE.STRING_OS_NAME)}));
					rc = 0;
				}
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (e.getMessage().contains("error=740") || e instanceof IOException) { //permission access exception //$NON-NLS-1$
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0023, new Object[] {command}));
				rc = 0; 
			}
			else if (e instanceof UnsatisfiedLinkError) {
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0037, new Object[] {e.getClass().getSimpleName()})
						+ Messages.getString(MessageIds.OSDE_MSGI0033));
			}
			else {
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0039));
			}
		}
		log.log(Level.INFO, "OpenSerialDataExplorer MIME registered = " + (rc == 0)); //$NON-NLS-1$
		return rc == 0;
	}

	/**
	 * register the application to the running OS
	 * associate file ending .osd as shell support
	 * @return true for successful registration
	 */
	public static boolean deregisterApplication() {
		int rc = -1;
		String targetDir = OSDE.JAVA_IO_TMPDIR;
		String command = OSDE.STRING_BLANK;	

		String jarBasePath = FileUtils.getOsdeJarBasePath();
		String jarFilePath = jarBasePath + "/OpenSerialDataExplorer.jar"; //$NON-NLS-1$

		try {
			JarFile jarFile = new JarFile(jarFilePath);

			if (OSDE.IS_WINDOWS) {
				// warn user for UAC or fail due to required admin rights accessing registry
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0029));
				String regExe = "Register" + OSDE.BIT_MODE + ".exe"; //$NON-NLS-1$ //$NON-NLS-2$ 
				log.log(Level.INFO, "register exe = " + regExe); //$NON-NLS-1$	

				FileUtils.extract(jarFile, regExe, OSDE.STRING_EMPTY, targetDir, "WIN"); //$NON-NLS-1$
				command = "cmd /C " + targetDir + regExe; //$NON-NLS-1$
				log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$	
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
				BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line;

				StringBuilder sb = new StringBuilder();
				while ((line = bisr.readLine()) != null) {
					sb.append(line);
				}
				log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
				sb = new StringBuilder();
				while ((line = besr.readLine()) != null) {
					sb.append(line);
				}
				log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
				if (process.exitValue() != 0) {
					String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-1$
					log.log(Level.SEVERE, msg);
					if (msg.contains("740"))
						throw new IOException("error=740");

					throw new UnsatisfiedLinkError(msg);
				}

				//check if deregistration was successful
				process = Runtime.getRuntime().exec("cmd /C assoc .osd"); //$NON-NLS-1$
				process.waitFor();
				bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				sb = new StringBuilder();
				while ((line = bisr.readLine()) != null) {
					sb.append(line);
				}
				log.log(Level.INFO, "std.out = " + sb.toString()); //$NON-NLS-1$
				sb = new StringBuilder();
				while ((line = besr.readLine()) != null) {
					sb.append(line);
				}
				log.log(Level.INFO, "std.err = " + sb.toString()); //$NON-NLS-1$
				log.log(Level.INFO, "\"cmd /C assoc .osd\" rc = " + process.exitValue()); //$NON-NLS-1$
				if (process.exitValue() == 0) {
					log.log(Level.WARNING, "failed to deregister OpenSerialData MIME type to OS rc = " + process.exitValue()); //$NON-NLS-1$
					throw new IOException("error=740");
				}
				rc = 0;
			}
			else if (OSDE.IS_LINUX) {
				String desktopFileName = "OpenSerialDataExplorer.desktop"; //$NON-NLS-1$
				String extractTargetFilePath = jarBasePath + desktopFileName;
				log.log(Level.INFO, "extractTargetFilePath = " + extractTargetFilePath); //$NON-NLS-1$
				File targetFile = new File(extractTargetFilePath);

				//installation directory must contain OpenSerialDataExplorer.desktop with write permission
				if (targetFile.exists() && targetFile.canWrite()) {
					FileUtils.extractWhileReplace("@OSDE_DIR@", jarBasePath, jarFilePath, desktopFileName, extractTargetFilePath, "UTF-8", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$);
					FileUtils.extract(jarFile, "unregister.sh", "", targetDir, "555"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					FileUtils.extract(jarFile, "OpenSerialDataExplorerdirectory", "", targetDir, "555"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

					command = "chmod +x " + targetDir + "/unregister.sh"; //$NON-NLS-1$ //$NON-NLS-2$
					log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
					Runtime.getRuntime().exec(command).waitFor();
					command = targetDir + "/unregister.sh"; //$NON-NLS-1$
					log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
					rc = Runtime.getRuntime().exec(command).waitFor();
				}
				else {
					// package error, must not occur in a deliverd driver
					log.log(Level.WARNING, extractTargetFilePath + " does not exist or does not have write (755) pernission, the OpenSerialDataExplorer MIME-type can not registered"); //$NON-NLS-1$
				}

				//check if xdg-utls are installed, this is the prerequisite for the registration process				
				if (Runtime.getRuntime().exec("which xdg-mime").waitFor() != 0) { //$NON-NLS-1$
					log.log(Level.INFO, "OpenSerialData program can not registered until xdg-utils are installed and in path"); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0030));
					rc = 0;
				}
			}
			else {
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0032, new Object[] {System.getProperty(OSDE.STRING_OS_NAME)}));
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (e.getMessage().contains("error=740") || e instanceof IOException) { //permission access exception //$NON-NLS-1$
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0023, new Object[] {command}));
				rc = 0; 
			}
			else if ( e instanceof UnsatisfiedLinkError) {
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0037, new Object[] {e.getClass().getSimpleName()})
						+ Messages.getString(MessageIds.OSDE_MSGI0033));
			}
			else {
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0039));
			}
		}
		return rc == 0;
	}
	
	/**
	 * create a file link
	 */
	public static void createFileLink(String fullQualifiedSourceFilePath, String fullQualifiedTargetFilePath) {
		
		if (OSDE.IS_WINDOWS) {
			try {
				fullQualifiedSourceFilePath = fullQualifiedSourceFilePath.replace(OSDE.FILE_SEPARATOR_UNIX, OSDE.FILE_SEPARATOR_WINDOWS);
				fullQualifiedTargetFilePath = fullQualifiedTargetFilePath.replace(OSDE.FILE_SEPARATOR_UNIX, OSDE.FILE_SEPARATOR_WINDOWS);
				String sourceBasePath = fullQualifiedSourceFilePath.substring(0, fullQualifiedSourceFilePath.lastIndexOf(OSDE.FILE_SEPARATOR_WINDOWS) + 1);
				log.log(Level.INFO, "sourceBasePath = " + sourceBasePath); //$NON-NLS-1$
				
				String targetFileLinkPath = fullQualifiedTargetFilePath.replace(OSDE.FILE_SEPARATOR_UNIX, OSDE.FILE_SEPARATOR_WINDOWS); // + ".lnk"; //$NON-NLS-1$
				log.log(Level.INFO, "targetFileLinkPath = " + targetFileLinkPath); //$NON-NLS-1$

				String[] shellLinkArgs = { targetFileLinkPath, fullQualifiedSourceFilePath, "", sourceBasePath, fullQualifiedSourceFilePath, "" };

				WindowsHelper.createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);
			}
			catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage());
			}
		}
		else if (OSDE.IS_LINUX) { //$NON-NLS-1$
			try {
				String fullQualifiedLinkTargetPath = fullQualifiedSourceFilePath.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
				if (fullQualifiedLinkTargetPath.contains(" ")) { // creating links with filenames containing blanks failed
					if (FileUtils.checkFileExist(fullQualifiedLinkTargetPath)) {
						File file = new File(fullQualifiedLinkTargetPath);
						fullQualifiedLinkTargetPath = fullQualifiedLinkTargetPath.replace(OSDE.STRING_BLANK, OSDE.STRING_UNDER_BAR);
						file.renameTo(new File(fullQualifiedLinkTargetPath));
					}
				}
				String fullQualifiedLinkPath = fullQualifiedTargetFilePath.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX).replace(OSDE.STRING_BLANK, OSDE.STRING_UNDER_BAR);
				String command = "ln -s " + fullQualifiedLinkTargetPath + OSDE.STRING_BLANK + fullQualifiedLinkPath; 
				log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
				BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line;
				while ((line = bisr.readLine()) != null) {
					log.log(Level.INFO, "std.out = " + line); //$NON-NLS-1$
				}
				while ((line = besr.readLine()) != null) {
					log.log(Level.INFO, "std.err = " + line); //$NON-NLS-1$
				}
				if (process.exitValue() != 0) {
					String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-1$
					log.log(Level.SEVERE, msg);
				}
			}
			catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage());
			}
		}
		else {
			log.log(Level.WARNING, "not supported OS"); //$NON-NLS-1$
			OpenSerialDataExplorer.getInstance().openMessageDialog("Operating System " + System.getProperty(OSDE.STRING_OS_NAME) + " is not supported!");
		}
	}
	
	/**
	 * check if the given file is a file link, if so it returns the contained file path
	 * @param filePath
	 * @return if shell link file the contained file path is returned, else the given file path is returned
	 * @throws IOException 
	 */
	public static String getLinkContainedFilePath(String filePath) throws IOException {
		String ret = filePath;
		if (OSDE.IS_WINDOWS) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath), "UTF-8")); //$NON-NLS-1$
			String line = reader.readLine();
			log.log(Level.INFO, "line = " + line);
			reader.close();
			if (!line.contains("OpenSerialData")) {
				ret = WindowsHelper.getFilePathFromLink(filePath);
				if (ret.startsWith("OSDE_MSGE")) {
					String msgKey = ret.split(";")[0];
					String msgValue = ret.split("; ")[1];
					throw new UnsatisfiedLinkError(Messages.getString(msgKey,
							new Object[] { msgValue }));
				}
			}
		}
		else if (OSDE.IS_LINUX) {
			try {
				String command = "ls -al " + filePath; 
				log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
				BufferedReader bisr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader besr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line;
				while ((line = bisr.readLine()) != null) {
					log.log(Level.INFO, "std.out = " + line); //$NON-NLS-1$
					if (line.contains(" -> ")) {
						ret = line.split(" -> ")[1].trim();
					}
				}
				while ((line = besr.readLine()) != null) {
					log.log(Level.INFO, "std.err = " + line); //$NON-NLS-1$
				}
				if (process.exitValue() != 0) {
					String msg = "failed to execute \"" + command + "\" rc = " + process.exitValue(); //$NON-NLS-1$ //$NON-NLS-1$
					log.log(Level.SEVERE, msg);
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}

		}
		else {
			log.log(Level.WARNING, "Operating System implementation not available");
		}
		return ret;
	}

}
