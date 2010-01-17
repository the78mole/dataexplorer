/**
 * 
 */
package osde.log;

/**
 * @author brueg
 *
 */
public class Level extends java.util.logging.Level {

  public static final java.util.logging.Level TIME = new Level("TIME", 850, "sun.util.logging.resources.logging");
  private static final long serialVersionUID = -8176160795706313070L;

	/**
	 * @param s
	 * @param i
	 */
	public Level(String s, int i) {
		super(s, i);
	}

	/**
	 * @param s
	 * @param i
	 * @param s1
	 */
	public Level(String s, int i, String s1) {
		super(s, i, s1);
	}

	
}
