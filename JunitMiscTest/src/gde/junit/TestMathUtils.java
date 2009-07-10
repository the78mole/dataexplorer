/**
 * 
 */
package osde.junit;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.utils.MathUtils;

/**
 * @author brueg
 *
 */
public class TestMathUtils  extends TestSuperClass {
	static Logger	log = Logger.getLogger(TestMathUtils.class.getName());

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();		
    log.setLevel(Level.INFO);
    log.setUseParentHandlers(true);
	}

	public void testRoundUpAlgorithm() {
		double cat[] = {0.0D, 1.0, 2.0, 4.0, 5.0, 8.0, 10.0, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, 300, 500, 740, 1000, 1345, 2000, 2569, 5000} ;

		for (double d : cat) {
			checkRoundUp(d);
		}
		for (double d : cat) {
			checkRoundUp(d * -1);
		}
	}

	/**
	 * @param cat
	 */
	private void checkRoundUp(double cat) {
		double value;
		if (cat > 0) {
			log.log(Level.INFO, cat + " roundUp " + MathUtils.roundUp(cat));
			value = cat + 0.17;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat + 0.27;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat + 0.42;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat + 0.57;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat + 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
		}
		else {
			log.log(Level.INFO, cat + " roundUp " + MathUtils.roundUp(cat));
			value = cat - 0.17;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat - 0.27;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat - 0.42;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat - 0.57;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = cat - 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
		}
	}

	public void testRoundDownAlgorithm() {
		double cat[] = {0.0D, 1.0, 2.0, 4.0, 5.0, 8.0, 10.0, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, 300, 500, 740, 1000, 1345, 2000, 2569, 5000} ;

		for (double d : cat) {
			checkRoundDown(d);
		}
		for (double d : cat) {
			checkRoundDown(d * -1);
		}
	}

	/**
	 * @param cat
	 */
	private void checkRoundDown(double cat) {
		double value;
		if (cat > 0) {
			log.log(Level.INFO, cat + " roundDown " + MathUtils.roundDown(cat));
			value = cat + 0.17;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat + 0.27;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat + 0.42;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat + 0.57;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat + 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundDown(value));
		}
		else {
			log.log(Level.INFO, cat + " roundDown " + MathUtils.roundDown(cat));
			value = cat - 0.17;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat - 0.27;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat - 0.42;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat - 0.57;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = cat - 0.77;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
		}
	}
	
	
}
