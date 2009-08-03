import java.util.Locale;

/**
 * 
 */

/**
 * @author brueg
 *
 */
public class FormatTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Double zahl = 234.5657687;
		System.out.println(String.format("%.2f", zahl));
		System.out.println(String.format(Locale.ENGLISH, "%.2f", zahl));
		System.out.println(String.format(Locale.US, "%.2f", zahl));
		System.out.println(String.format(Locale.GERMAN, "%.2f", zahl));
		System.out.println(String.format(Locale.GERMANY, "%.2f", zahl));
		System.out.println(""+(((int)(100.0*zahl))/100.0));
	}

}
