/**
 * 
 */
package osde.junit;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author brueg
 *
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for osde.junit");
		//$JUnit-BEGIN$
		suite.addTestSuite(LogViewReaderTester.class);
		//$JUnit-END$
		return suite;
	}

}
