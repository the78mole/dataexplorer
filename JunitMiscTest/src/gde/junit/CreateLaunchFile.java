/**
 * 
 */
package gde.junit;

import junit.framework.TestCase;
import gde.utils.OperatingSystemHelper;

/**
 * @author brueg
 *
 */
public class CreateLaunchFile extends TestCase {

	public void testCreateShortCut() {
		OperatingSystemHelper.createDesktopLink();
	}
}
