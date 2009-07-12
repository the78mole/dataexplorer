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
	
	double cat[] = {0,0, 0.22, 0.45, 0.81, 0.90, 1.0, 1.12, 1.87, 2.0, 2.12, 2.54, 3.87, 4.0, 4.33, 4.54, 4.91, 5.0, 5.12, 7.34, 8.0, 10.0, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, 300, 500, 740, 1000, 1345, 2000, 2569, 5000} ;


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

		for (double d : this.cat) {
			checkRoundUp(d);
		}
		for (double d : this.cat) {
			checkRoundUp(d * -1);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundUp(double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " roundUp " + MathUtils.roundUp(category));
			value = category + 0.17;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category + 0.27;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category + 0.42;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category + 0.57;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category + 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
		}
		else {
			log.log(Level.INFO, category + " roundUp " + MathUtils.roundUp(category));
			value = category - 0.17;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category - 0.27;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category - 0.42;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category - 0.57;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
			value = category - 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value));
		}
	}

	public void testRoundDownAlgorithm() {

		for (double d : this.cat) {
			checkRoundDown(d);
		}
		for (double d : this.cat) {
			checkRoundDown(d * -1);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundDown(double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " roundDown " + MathUtils.roundDown(category));
			value = category + 0.17;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category + 0.27;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category + 0.42;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category + 0.57;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category + 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundDown(value));
		}
		else {
			log.log(Level.INFO, category + " roundDown " + MathUtils.roundDown(category));
			value = category - 0.17;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category - 0.27;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category - 0.42;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category - 0.57;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
			value = category - 0.77;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value));
		}
	}
	

	public void testRoundUpAlgorithmWithDeltaval() {

		for (double d : this.cat) {
			checkRoundUp(d,d);
		}
		for (double d : this.cat) {
			checkRoundUp(d * -1,d);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundUp(double val, double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " roundUp " + MathUtils.roundUp(category, category));
			value = val + 0.17;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val + 0.27;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val + 0.42;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val + 0.57;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val + 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
		}
		else {
			log.log(Level.INFO, category + " roundUp " + MathUtils.roundUp(category, category));
			value = val - 0.17;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val - 0.27;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val - 0.42;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val - 0.57;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
			value = val - 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundUp(value, category));
		}
	}

	public void testRoundDownAlgorithmWithDeltaVal() {

		for (double d : this.cat) {
			checkRoundDown(d, d);
		}
		for (double d : this.cat) {
			checkRoundDown(d * -1, d);
		}
	}

	/**
	 * @param category
	 */
	private void checkRoundDown(double val, double category) {
		double value;
		if (category > 0) {
			log.log(Level.INFO, category + " roundDown " + MathUtils.roundDown(category, category));
			value = val + 0.17;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val + 0.27;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val + 0.42;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val + 0.57;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val + 0.77;
			log.log(Level.INFO, value + " roundUp " + MathUtils.roundDown(value, category));
		}
		else {
			log.log(Level.INFO, category + " roundDown " + MathUtils.roundDown(category, category));
			value = val - 0.17;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val - 0.27;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val - 0.42;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val - 0.57;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
			value = val - 0.77;
			log.log(Level.INFO, value + " roundDown " + MathUtils.roundDown(value, category));
		}
	}
	
}
