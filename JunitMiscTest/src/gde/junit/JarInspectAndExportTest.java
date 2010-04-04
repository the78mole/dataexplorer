/**
 * 
 */
package osde.junit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.TestCase;
import osde.DE;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.utils.FileUtils;

/**
 * @author brueg
 *
 */
public class JarInspectAndExportTest extends TestCase {
	String	applHomePath	= null;
	String	osname				= System.getProperty("os.name", "").toLowerCase();	//$NON-NLS-1$ //$NON-NLS-2$
	Locale	locale;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		if (this.osname.startsWith("windows")) { //$NON-NLS-1$
			this.applHomePath = (System.getenv("APPDATA") + DE.FILE_SEPARATOR_UNIX + "DataExplorer").replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		else if (this.osname.startsWith("linux")) { //$NON-NLS-1$
			this.applHomePath = System.getProperty("user.home") + DE.FILE_SEPARATOR_UNIX + ".DataExplorer"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			System.err.println(Messages.getString(MessageIds.DE_MSGW0001));
			return;
		}
		System.out.println("applHomePath = " + this.applHomePath);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testListDeviceJars() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

			File sourceDir = new File(FileUtils.getDevicePluginJarBasePath());
			String[] files = sourceDir.list();
			for (String jarName : files) {
				System.out.println(jarName);
			}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	public void testListDeviceJarsManifest() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		String jarFileDir = FileUtils.getDevicePluginJarBasePath();
		File sourceDir = new File(jarFileDir);
		String[] files = sourceDir.list();
		for (String fileName : files) {
			if (!fileName.endsWith(".jar")) continue;
			try {
				JarFile jf = new JarFile(jarFileDir + "/" + fileName);
				Manifest m = jf.getManifest();
				System.out.println("\n" + fileName);
				for (Object key : m.getMainAttributes().keySet()) {
					System.out.println(key + ": " + m.getMainAttributes().get(key));
				}
			}
			catch (IOException e) {
				failures.put(e.getMessage(), e);
			}
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	public void testExtractDevicePictures() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		String jarFileDir = FileUtils.getDevicePluginJarBasePath();
		File sourceDir = new File(jarFileDir);
		String[] files = sourceDir.list();
		try {
			for (String jarFileName : files) {
				if (!jarFileName.endsWith(".jar")) continue;
				JarFile jarFile = new JarFile(jarFileDir + "/" + jarFileName);
				String[] plugins = FileUtils.getDeviceJarServicesNames(jarFile);
				for (String plugin : plugins) {
					String targetDirectory = System.getProperty("java.io.tmpdir");
					FileUtils.extract(jarFile, plugin + ".jpg", "resource/", targetDirectory, "555");
				}
			}
		}
		catch (IOException e) {
			failures.put(e.getMessage(), e);
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	public void testExtractDeviceProperties() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		String jarFileDir = FileUtils.getDevicePluginJarBasePath();
		File sourceDir = new File(jarFileDir);
		String[] files = sourceDir.list();
		try {
			for (String jarFileName : files) {
				if (!jarFileName.endsWith(".jar")) continue;
				JarFile jarFile = new JarFile(jarFileDir + "/" + jarFileName);
				String[] plugins = FileUtils.getDeviceJarServicesNames(jarFile);
				for (String plugin : plugins) {
					String targetDirectory = System.getProperty("java.io.tmpdir");
					FileUtils.extract(jarFile, plugin + ".xml", "resource/en/", targetDirectory, "555");
				}
			}
		}
		catch (IOException e) {
			failures.put(e.getMessage(), e);
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

}
