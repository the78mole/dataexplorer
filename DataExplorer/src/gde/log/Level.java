/**
 * 
 */
package osde.log;

/**
 * @author brueg
 *
 */
public class Level extends java.util.logging.Level {

  public static final Level SEVERE;
  public static final Level WARNING;
  public static final Level TIME;
  private static final long serialVersionUID = -8176160795706313070L;

  static 
  {
      //defaultBundle = "sun.util.logging.resources.logging";
      //OFF = new Level("OFF", 2147483647, defaultBundle);
      SEVERE = new Level("SEVERE", 1100, "sun.util.logging.resources.logging");
      WARNING = new Level("WARNING", 1000, "sun.util.logging.resources.logging");
      TIME = new Level("TIME", 900, "sun.util.logging.resources.logging");
      //INFO = new Level("INFO", 800, defaultBundle);
      //CONFIG = new Level("CONFIG", 700, defaultBundle);
      //FINE = new Level("FINE", 500, defaultBundle);
      //FINER = new Level("FINER", 400, defaultBundle);
      //FINEST = new Level("FINEST", 300, defaultBundle);
      //ALL = new Level("ALL", -2147483648, defaultBundle);
  }

	/**
	 * @param s
	 * @param i
	 */
	public Level(String s, int i) {
		super(s, i);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param s
	 * @param i
	 * @param s1
	 */
	public Level(String s, int i, String s1) {
		super(s, i, s1);
		// TODO Auto-generated constructor stub
	}

	
}
