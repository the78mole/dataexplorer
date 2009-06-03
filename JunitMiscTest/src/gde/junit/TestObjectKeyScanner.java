/**
 * 
 */
package osde.junit;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.utils.ObjectKeyScanner;

/**
 * @author brueg
 *
 */
public class TestObjectKeyScanner extends TestSuperClass {
	Logger	logger = Logger.getLogger("osde.utils.ObjectKeyScanner");

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();		
    this.logger.setLevel(Level.FINER);
    this.logger.setUseParentHandlers(true);
	}
	
	public final void testFindAllObjectKeysAndCreateLinks() {
		ObjectKeyScanner objLnkSearch = new ObjectKeyScanner();
		objLnkSearch.setSearchForKeys(true);
		objLnkSearch.start();
		while(objLnkSearch.isAlive()) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	public final void testCreateLinksForObjectKey() {
		ObjectKeyScanner objLnkCrt = new ObjectKeyScanner("ASW-27");
		objLnkCrt.start();
		while(objLnkCrt.isAlive()) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

}
