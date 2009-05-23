/**
 * 
 */
package osde.junit;

import junit.framework.TestCase;
import osde.utils.OperatingSystemHelper;

/**
 * @author brueg
 *
 */
public class CreateLaunchFile extends TestCase {

	public void testCreateShortCut() {
		OperatingSystemHelper.createDesktopLink();
	}
}
